package ru.itmo.vehicle.repo;

import ru.itmo.vehicle.domain.FuelType;
import ru.itmo.vehicle.domain.Vehicle;
import ru.itmo.vehicle.domain.VehicleType;

import java.util.List;
import java.util.Optional;

/**
 * Интерфейс репозитория (слой хранения). Отделяет бизнес-логику от JPA/EclipseLink.
 */
public interface VehicleRepository {

    Vehicle save(Vehicle vehicle);

    Optional<Vehicle> findById(Long id);

    List<Vehicle> findAll();

    /** Пагинация + сортировка + фильтрация на уровне БД. */
    List<Vehicle> findPage(String filterField, String filterValue, String sortField, boolean asc, int offset, int limit);

    long countFiltered(String filterField, String filterValue);

    void deleteById(Long id);

    /* ---------- специальные операции (реализованы через JPQL, но вызываются из бизнес-логики) ---------- */

    double sumFuelConsumption();

    Optional<Vehicle> minByType();

    long countByFuelTypeGreaterThan(FuelType fuelType);

    List<Vehicle> findByType(VehicleType type);

    List<Vehicle> findByCoordinatesId(Long coordinatesId);
}
