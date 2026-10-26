package ru.itmo.vehicle.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.vehicle.domain.Coordinates;
import ru.itmo.vehicle.repo.CoordinatesRepository;
import ru.itmo.vehicle.exception.ValidationException;

import java.util.List;

@Service
public class CoordinatesService {
    private final CoordinatesRepository repository;

    public CoordinatesService(CoordinatesRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Coordinates> findAll() {
        return repository.findAll();
    }

    @Transactional
    public Coordinates create(Coordinates coordinates) {
        if (coordinates.getX() == null) {
            throw new ValidationException(java.util.Map.of("x", "Поле x не может быть null"));
        }
        return repository.save(coordinates);
    }
}
