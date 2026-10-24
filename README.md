# Vehicle Information System

Информационная система для управления объектами класса `Vehicle` (транспортные средства).
Реализована как Spring MVC приложение с разделением на слои, ORM EclipseLink и базой данных PostgreSQL.
Сборка — Maven, пакет — WAR, сервер приложений — WildFly 23 (Java 17).

## Содержание

- [Предметная область](#предметная-область)
- [Стек](#стек)
- [Структура проекта](#структура-проекта)
- [Сборка и тесты](#сборка-и-тесты)
- [База данных](#база-данных)
- [Развёртывание на Helios](#развёртывание-на-helios)
- [Веб-интерфейс](#веб-интерфейс)
- [REST API](#rest-api)
- [Специальные операции](#специальные-операции)
- [Особенности реализации](#особенности-реализации)

## Предметная область

### Vehicle

| Поле | Тип | Ограничения |
| --- | --- | --- |
| `id` | `Long` | генерируется БД, уникальное, `> 0` |
| `name` | `String` | не `null`, не пустая строка |
| `coordinates` | `Coordinates` | не `null`, связь многие-к-одному |
| `creationDate` | `ZonedDateTime` | заполняется автоматически, не `null`, не изменяется |
| `type` | `VehicleType` | может быть `null` |
| `enginePower` | `double` | `> 0` |
| `numberOfWheels` | `Long` | может быть `null`, иначе `> 0` |
| `capacity` | `Long` | не `null`, `> 0` |
| `distanceTravelled` | `float` | `> 0` |
| `fuelConsumption` | `double` | `> 0` |
| `fuelType` | `FuelType` | может быть `null` |

`VehicleType`: `DRONE`, `BOAT`, `SHIP`, `BICYCLE`.

`FuelType`: `GASOLINE`, `KEROSENE`, `ELECTRICITY`, `NUCLEAR`, `ANTIMATTER`.

Порядок объявления значений `FuelType` используется в специальной операции сравнения:
`GASOLINE < KEROSENE < ELECTRICITY < NUCLEAR < ANTIMATTER`.

### Coordinates

| Поле | Тип | Ограничения |
| --- | --- | --- |
| `id` | `Long` | генерируется БД, уникальное, `> 0` |
| `x` | `Double` | не `null`, `<= 862` |
| `y` | `int` | `> -860` |

Один объект `Coordinates` может быть связан с несколькими `Vehicle`.
При удалении `Coordinates` связанные транспортные средства переназначаются на другой объект.

## Стек

- Java 17
- Spring Framework 5.3.39: Spring MVC, Spring Context, Spring ORM, Spring TX
- EclipseLink 2.7.14 (JPA 2.2)
- PostgreSQL (драйвер 42.6.2)
- Jackson 2.15.4 (`jackson-databind`, `jackson-datatype-jsr310`)
- Hibernate Validator 6.2.5 (Bean Validation 2.0)
- JUnit 5.10.2, Mockito 5.11.0
- Maven WAR Plugin
- WildFly 23.0.2.Final
- Клиентская часть: HTML + CSS + vanilla JavaScript (без сборщика)

## Структура проекта

```
src/
  main/
    java/ru/itmo/vehicle/
      config/        конфигурация Spring, инициализация DispatcherServlet
      domain/        JPA-сущности, enum-типы, конвертер даты
      repo/          репозитории (JPQL через EntityManager)
      service/       бизнес-логика, валидация, события
      web/           REST-контроллеры, DTO, обработчик ошибок
    resources/
      META-INF/persistence.xml   конфигурация EclipseLink
      schema-postgres.sql        DDL для PostgreSQL
    webapp/
      static/index.html          одностраничный интерфейс
      static/js/app.js           логика интерфейса
      static/css/style.css       стили
  test/
    java/ru/itmo/vehicle/VehicleServiceTest.java   модульные тесты
pom.xml
```

## Сборка и тесты

Требуется JDK 17 и Maven 3.8+.

```bash
mvn clean package
```

Результат сборки: `target/vehicle-is.war`.

Только тесты:

```bash
mvn test
```

Тесты покрывают валидацию, создание/обновление/удаление, специальные операции,
добавление колёс и ограничения `Coordinates`.

## База данных

Параметры подключения задаются в `src/main/resources/META-INF/persistence.xml`:

```
jdbc:postgresql://pg:5432/studs
```

Имя пользователя и пароль берутся из переменных окружения `DB_USER` и `DB_PASSWORD`
(см. `AppConfig`). Если переменные не заданы, используются системные свойства с теми же именами.

Схема создаётся скриптом `src/main/resources/schema-postgres.sql`:

```bash
psql -h pg -U "$DB_USER" -d studs -f src/main/resources/schema-postgres.sql
```

Скрипт создаёт таблицы `coordinates` и `vehicle`, индексы и CHECK-ограничения,
повторяющие ограничения предметной области. Скрипт идемпотентен
(`CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`).

`persistence.xml` содержит настройки EclipseLink для генерации DDL, но с
`eclipselink.ddl-generation.output-mode=none`, поэтому схема автоматически не изменяется.
Перед первым запуском примените `schema-postgres.sql`.

## Развёртывание на Helios

### 1. Сборка

```bash
mvn clean package
```

### 2. Копирование WAR на сервер

```bash
scp -P 2222 target/vehicle-is.war \
  s434931@helios.cs.ifmo.ru:~/vehicle-runtime/wildfly-23.0.2.Final/standalone/deployments/
```

### 3. Запуск WildFly

Подключиться к серверу:

```bash
ssh -p 2222 s434931@helios.cs.ifmo.ru
```

Выполнить:

```bash
cd ~/vehicle-runtime/wildfly-23.0.2.Final

export JAVA_HOME=/usr/local/openjdk17

export DB_USER=s434931
export DB_PASSWORD="$(awk -F: '$4=="s434931" {print $5; exit}' ~/.pgpass)"

export _JAVA_OPTIONS="-Xms64m -Xmx512m -XX:MaxMetaspaceSize=512m"

bin/standalone.sh -c standalone-test.xml -Djboss.socket.binding.port-offset=100
```

`JAVA_HOME` обязателен: без него WildFly может взять неподходящую версию Java.
`_JAVA_OPTIONS` ограничивает память процесса, иначе контейнер может быть остановлен
из-за превышения лимита.

Смещение портов `100` означает:

- HTTP: `8080 + 100 = 8180`
- Management: `9990 + 100 = 10090`

### 4. SSH-туннель

На локальной машине:

```bash
ssh -p 2222 -L 8180:127.0.0.1:8180 s434931@helios.cs.ifmo.ru
```

После этого приложение доступно по адресу:

```
http://localhost:8180/vehicle-is/
```

Корневой URL перенаправляется на `/vehicle-is/static/index.html`.

### Остановка

В консоли WildFly — `Ctrl+C`. Перед повторным запуском убедиться, что порт `8180`
не занят предыдущим процессом.

## Веб-интерфейс

Одностраничное приложение на hash-маршрутах:

- `#/` — список транспортных средств;
- `#/view/:id` — подробная информация об объекте;
- `#/special` — специальные операции;
- `#/new` — открывает диалог создания объекта.

Возможности:

- постраничный вывод (5, 8, 10, 20 или 50 строк);
- сортировка по клику на заголовок любой колонки (повторный клик меняет направление);
- фильтрация по выбранной колонке с частичным совпадением подстроки;
- создание и редактирование в модальном диалоге;
- клиентская и серверная валидация с подсветкой конкретных полей;
- выбор существующего `Coordinates` или создание нового вместе с `Vehicle`;
- просмотр связанного объекта `Coordinates` на странице деталей;
- удаление с подтверждением;
- автоматическое обновление списка и страницы деталей при изменениях,
  сделанных другим клиентом (Server-Sent Events);
- индикатор состояния SSE-соединения в шапке;
- удаление `Coordinates` с переназначением связанных `Vehicle`.

## REST API

Базовый префикс: `/vehicle-is/api`.

### Транспортные средства

| Метод | Путь | Описание |
| --- | --- | --- |
| `GET` | `/vehicles` | постраничный список |
| `POST` | `/vehicles` | создать объект, ответ `201` |
| `GET` | `/vehicles/{id}` | получить объект |
| `PUT` | `/vehicles/{id}` | обновить объект |
| `DELETE` | `/vehicles/{id}` | удалить объект |
| `GET` | `/vehicles/events` | поток SSE-событий |

Параметры списка:

| Параметр | По умолчанию | Описание |
| --- | --- | --- |
| `filterField` | — | колонка фильтра |
| `filterValue` | — | подстрока для частичного совпадения |
| `sortField` | `id` | колонка сортировки |
| `asc` | `true` | направление сортировки |
| `page` | `1` | номер страницы, начиная с 1 |
| `size` | `10` | размер страницы, от 1 до 100 |

Доступные поля фильтрации и сортировки:
`id`, `name`, `type`, `fuelType`, `creationDate`, `enginePower`, `numberOfWheels`,
`capacity`, `distanceTravelled`, `fuelConsumption`, `coordX`, `coordY`.

Фильтрация по строковым полям (`name`, `type`, `fuelType`) выполняется без учёта регистра.
Для остальных полей значение приводится к строке и сравнивается как подстрока.

Пример ответа списка:

```json
{
  "items": [],
  "total": 0,
  "page": 1,
  "size": 10
}
```

Тело создания и обновления (`VehicleDto`):

```json
{
  "name": "Quadrocopter",
  "coordinates": { "id": null, "x": 12.5, "y": 7 },
  "type": "DRONE",
  "enginePower": 15.5,
  "numberOfWheels": 4,
  "capacity": 2,
  "distanceTravelled": 120.0,
  "fuelConsumption": 3.5,
  "fuelType": "ELECTRICITY"
}
```

Если у `coordinates` задан `id`, используется существующий объект `Coordinates`.
Если `id` равен `null`, создаётся новый объект.

Ошибки валидации возвращаются с кодом `400`:

```json
{
  "errors": {
    "coordinates.x": "Максимальное значение поля x: 862"
  }
}
```

### Coordinates

| Метод | Путь | Описание |
| --- | --- | --- |
| `GET` | `/coordinates` | список всех объектов |
| `POST` | `/coordinates` | создать объект |
| `DELETE` | `/coordinates/{id}?replacementId=N` | переназначить связанные `Vehicle` и удалить объект |

### SSE

`GET /vehicles/events` возвращает поток `text/event-stream`.
Имена событий: `created`, `updated`, `deleted`.
Данные события:

```json
{
  "vehicle": {
    "id": 1,
    "name": "Quadrocopter"
  }
}
```

События публикуются только после успешного коммита транзакции, поэтому клиент
получает уже сохранённый идентификатор объекта. Механизм работает в пределах
одного экземпляра WildFly.

## Специальные операции

| Метод | Путь | Описание | Ответ |
| --- | --- | --- | --- |
| `GET` | `/special/fuel-consumption-sum` | сумма `fuelConsumption` всех объектов | `{"result": 12.5}` |
| `GET` | `/special/min-by-type` | объект с минимальным `type` (по порядку enum) | `VehicleDto` или `404` |
| `GET` | `/special/count-fuel-type-greater?fuelType=X` | количество объектов, у которых `fuelType` больше `X` | `{"result": 3}` |
| `GET` | `/special/by-type?type=X` | все объекты заданного `VehicleType` | `{"items": [...]}` |
| `POST` | `/special/{id}/wheels?count=N` | добавить `N` колёс объекту с `id` | обновлённый `VehicleDto` |

Для `min-by-type` объекты с `type = null` не учитываются.
Для `count-fuel-type-greater` объекты с `fuelType = null` не учитываются,
а сравнение идёт по порядку объявления значений `FuelType`.

## Особенности реализации

- Слои разделены: `web` → `service` → `repo` → `domain`.
- Транзакциями управляет Spring (`@Transactional`, `JpaTransactionManager`).
- `creationDate` выставляется в `@PrePersist` и не обновляется при редактировании.
- Идентификатор нового объекта генерируется PostgreSQL (`GENERATED ALWAYS AS IDENTITY`).
- Bean Validation дополнена проверками в сеттерах доменных классов.
- Фильтрация и сортировка строятся по белому списку полей, поэтому имена колонок
  не подставляются в JPQL напрямую из запроса.
- Даты сериализуются как строки; для `creationDate` задан шаблон
  `yyyy-MM-dd HH:mm:ss z`.
- Статические ресурсы отдаются Spring MVC из `/static/**`.
- Ошибки разбора JSON обрабатываются `ApiExceptionHandler` и возвращаются в едином формате.
