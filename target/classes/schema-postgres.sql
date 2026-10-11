-- Схема БД для ИС «Транспортные средства» (PostgreSQL, схема по имени пользователя)
-- id генерируется средствами БД (GENERATED ALWAYS AS IDENTITY).
-- Все ограничения из описания класса реализованы на уровне БД.

CREATE TABLE IF NOT EXISTS coordinates (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    x      DOUBLE PRECISION NOT NULL,
    y      INTEGER NOT NULL,
    CONSTRAINT chk_coordinates_id_positive CHECK (id > 0),
    CONSTRAINT chk_coordinates_x_max CHECK (x <= 862),
    CONSTRAINT chk_coordinates_y_min CHECK (y > -860)
);

CREATE TABLE IF NOT EXISTS vehicle (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name              VARCHAR(500) NOT NULL,
    coordinates_id    BIGINT NOT NULL REFERENCES coordinates(id),
    creation_date     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    type              VARCHAR(20),
    engine_power      DOUBLE PRECISION NOT NULL,
    number_of_wheels  BIGINT,
    capacity          BIGINT NOT NULL,
    distance_travelled REAL NOT NULL,
    fuel_consumption  DOUBLE PRECISION NOT NULL,
    fuel_type         VARCHAR(20),

    CONSTRAINT chk_vehicle_id_positive CHECK (id > 0),
    CONSTRAINT chk_vehicle_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT chk_engine_power_positive CHECK (engine_power > 0),
    CONSTRAINT chk_wheels_positive CHECK (number_of_wheels IS NULL OR number_of_wheels > 0),
    CONSTRAINT chk_capacity_positive CHECK (capacity > 0),
    CONSTRAINT chk_distance_positive CHECK (distance_travelled > 0),
    CONSTRAINT chk_fuel_consumption_positive CHECK (fuel_consumption > 0),
    CONSTRAINT chk_type_enum CHECK (type IS NULL OR type IN ('DRONE','BOAT','SHIP','BICYCLE')),
    CONSTRAINT chk_fueltype_enum CHECK (fuel_type IS NULL OR fuel_type IN ('GASOLINE','KEROSENE','ELECTRICITY','NUCLEAR','ANTIMATTER'))
);

CREATE INDEX IF NOT EXISTS idx_vehicle_name ON vehicle (name);
CREATE INDEX IF NOT EXISTS idx_vehicle_coordinates ON vehicle (coordinates_id);
