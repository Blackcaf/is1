package ru.itmo.vehicle.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.vehicle.domain.FuelType;
import ru.itmo.vehicle.domain.Coordinates;
import ru.itmo.vehicle.domain.Vehicle;
import ru.itmo.vehicle.domain.VehicleType;
import ru.itmo.vehicle.repo.VehicleRepository;
import ru.itmo.vehicle.repo.CoordinatesRepository;

import javax.validation.ConstraintViolation;
import javax.validation.groups.Default;
import javax.validation.Validator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository repository;

    @Autowired(required = false)
    private CoordinatesRepository coordinatesRepository;

    @Autowired
    private Validator validator;

    @Autowired
    private VehicleEventBus eventBus;

    @Transactional
    public Vehicle create(Vehicle vehicle) {
        validate(vehicle);
        resolveCoordinates(vehicle);
        vehicle.setId(null);
        vehicle.setCreationDate(null);
        Vehicle saved = repository.save(vehicle);
        eventBus.publish(VehicleEventBus.Action.CREATED, copyOf(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Vehicle> getById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Vehicle update(Long id, Vehicle changes) {
        Vehicle existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Транспортное средство с id=" + id + " не найдено"));
        changes.setId(existing.getId());
        changes.setCreationDate(existing.getCreationDate());
        validate(changes);
        resolveCoordinates(changes);
        Vehicle saved = repository.save(changes);
        eventBus.publish(VehicleEventBus.Action.UPDATED, copyOf(saved));
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Vehicle v = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Транспортное средство с id=" + id + " не найдено"));
        repository.deleteById(id);
        eventBus.publish(VehicleEventBus.Action.DELETED, copyOf(v));
    }

    @Transactional(readOnly = true)
    public List<Vehicle> page(String filterField, String filterValue, String sortField, boolean asc,
                              int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        return repository.findPage(filterField, filterValue, sortField, asc, (p - 1) * s, s);
    }

    @Transactional(readOnly = true)
    public long count(String filterField, String filterValue) {
        return repository.countFiltered(filterField, filterValue);
    }

    @Transactional(readOnly = true)
    public double sumFuelConsumption() {
        return repository.sumFuelConsumption();
    }

    @Transactional(readOnly = true)
    public Optional<Vehicle> minByType() {
        List<Vehicle> all = repository.findAll();
        Vehicle best = null;
        for (Vehicle v : all) {
            if (v.getType() == null) continue;
            if (best == null || v.getType().ordinal() < best.getType().ordinal()) {
                best = v;
            }
        }
        return Optional.ofNullable(best);
    }

    @Transactional(readOnly = true)
    public long countFuelTypeGreaterThan(FuelType fuelType) {
        if (fuelType == null) {
            throw new ValidationException(Map.of("fuelType", "Не задан тип топлива для сравнения"));
        }
        long count = 0;
        for (Vehicle v : repository.findAll()) {
            if (v.getFuelType() != null && v.getFuelType().ordinal() > fuelType.ordinal()) {
                count++;
            }
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findByType(VehicleType type) {
        if (type == null) {
            throw new ValidationException(Map.of("type", "Не задан тип транспортного средства"));
        }
        return repository.findByType(type);
    }

    @Transactional
    public Vehicle addWheels(Long id, long wheelsToAdd) {
        if (wheelsToAdd <= 0) {
            throw new ValidationException(Map.of("wheelsToAdd", "Число добавляемых колёс должно быть больше 0"));
        }
        Vehicle v = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Транспортное средство с id=" + id + " не найдено"));
        long current = v.getNumberOfWheels() == null ? 0L : v.getNumberOfWheels();
        v.setNumberOfWheels(current + wheelsToAdd);
        Vehicle saved = repository.save(v);
        eventBus.publish(VehicleEventBus.Action.UPDATED, copyOf(saved));
        return saved;
    }

    @Transactional
    public void deleteCoordinates(Long coordinatesId, Long replacementId) {
        if (coordinatesRepository == null) {
            throw new IllegalStateException("Репозиторий Coordinates не настроен");
        }
        Coordinates oldCoordinates = coordinatesRepository.findById(coordinatesId)
                .orElseThrow(() -> new NotFoundException("Coordinates с id=" + coordinatesId + " не найден"));
        Coordinates replacement = coordinatesRepository.findById(replacementId)
                .orElseThrow(() -> new NotFoundException("Coordinates с id=" + replacementId + " не найден"));
        if (coordinatesId.equals(replacementId)) {
            throw new ValidationException(Map.of("replacementId", "Нужно выбрать другой объект Coordinates"));
        }
        for (Vehicle vehicle : repository.findByCoordinatesId(coordinatesId)) {
            vehicle.setCoordinates(replacement);
            repository.save(vehicle);
            eventBus.publish(VehicleEventBus.Action.UPDATED, copyOf(vehicle));
        }
        coordinatesRepository.delete(oldCoordinates);
    }

    public void validate(Vehicle vehicle) {
        Set<ConstraintViolation<Vehicle>> violations = validator.validate(vehicle, Default.class);
        if (vehicle.getId() == null) {
            violations.removeIf(cv -> {
                String path = cv.getPropertyPath().toString();
                return path.equals("creationDate") || path.equals("id");
            });
        }

        if (!violations.isEmpty()) {
            Map<String, String> errors = new TreeMap<>();
            for (ConstraintViolation<Vehicle> cv : violations) {
                errors.putIfAbsent(cv.getPropertyPath().toString(), cv.getMessage());
            }
            throw new ValidationException(errors);
        }
        if (vehicle.getName() == null || vehicle.getName().isBlank()) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("name", "Поле name не может быть null, строка не может быть пустой");
            throw new ValidationException(errors);
        }
        if (vehicle.getCoordinates() == null) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("coordinates", "Поле coordinates не может быть null");
            throw new ValidationException(errors);
        }
    }

    private void resolveCoordinates(Vehicle vehicle) {
        Coordinates coordinates = vehicle.getCoordinates();
        if (coordinates == null || coordinates.getId() == null || coordinatesRepository == null) {
            return;
        }
        Coordinates existing = coordinatesRepository.findById(coordinates.getId())
                .orElseThrow(() -> new ValidationException(
                        Map.of("coordinates", "Выбранный объект Coordinates не найден")));
        vehicle.setCoordinates(existing);
    }

    static Vehicle copyOf(Vehicle src) {
        Vehicle c = new Vehicle();
        c.setId(src.getId());
        c.setName(src.getName());
        if (src.getCoordinates() != null) {
            c.setCoordinates(new ru.itmo.vehicle.domain.Coordinates(src.getCoordinates().getX(),
                    src.getCoordinates().getY()));
            c.getCoordinates().setId(src.getCoordinates().getId());
        }
        c.setCreationDate(src.getCreationDate());
        c.setType(src.getType());
        c.setEnginePower(src.getEnginePower());
        c.setNumberOfWheels(src.getNumberOfWheels());
        c.setCapacity(src.getCapacity());
        c.setDistanceTravelled(src.getDistanceTravelled());
        c.setFuelConsumption(src.getFuelConsumption());
        c.setFuelType(src.getFuelType());
        return c;
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }
}
