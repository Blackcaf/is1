package ru.itmo.vehicle.repo;

import ru.itmo.vehicle.domain.Vehicle;
import ru.itmo.vehicle.domain.VehicleType;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository {

    Vehicle save(Vehicle vehicle);

    Optional<Vehicle> findById(Long id);

    List<Vehicle> findAll();

    List<Vehicle> findPage(String filterField, String filterValue, String sortField, boolean asc, int offset, int limit);

    long countFiltered(String filterField, String filterValue);

    void deleteById(Long id);

    double sumFuelConsumption();

    List<Vehicle> findByType(VehicleType type);

    List<Vehicle> findByCoordinatesId(Long coordinatesId);
}
