package ru.itmo.vehicle.repo;

import ru.itmo.vehicle.domain.Coordinates;

import java.util.List;
import java.util.Optional;

public interface CoordinatesRepository {
    List<Coordinates> findAll();
    Optional<Coordinates> findById(Long id);
    Coordinates save(Coordinates coordinates);
    void delete(Coordinates coordinates);
}
