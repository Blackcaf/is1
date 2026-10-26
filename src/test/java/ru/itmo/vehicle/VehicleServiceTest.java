package ru.itmo.vehicle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import ru.itmo.vehicle.domain.Coordinates;
import ru.itmo.vehicle.domain.FuelType;
import ru.itmo.vehicle.domain.Vehicle;
import ru.itmo.vehicle.domain.VehicleType;
import ru.itmo.vehicle.repo.VehicleRepository;
import ru.itmo.vehicle.repo.CoordinatesRepository;
import ru.itmo.vehicle.exception.ValidationException;
import ru.itmo.vehicle.service.VehicleEventBus;
import ru.itmo.vehicle.service.VehicleService;
import ru.itmo.vehicle.exception.ResourceNotFoundException;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleServiceTest {

    private VehicleService service;
    private VehicleRepository repository;
    private VehicleEventBus eventBus;
    private CoordinatesRepository coordinatesRepository;

    private static Vehicle vehicle(Long id, String name, VehicleType type, FuelType fuel, double fc) {
        Vehicle v = new Vehicle();
        v.setId(id);
        v.setName(name);
        v.setCoordinates(new Coordinates(1.0, 2));
        v.setType(type);
        v.setEnginePower(100);
        v.setCapacity(4L);
        v.setDistanceTravelled(10f);
        v.setFuelConsumption(fc);
        v.setFuelType(fuel);
        return v;
    }

    @BeforeEach
    void setUp() {
        repository = mock(VehicleRepository.class);
        CoordinatesRepository coordinatesRepository = mock(CoordinatesRepository.class);
        eventBus = new VehicleEventBus();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        service = new VehicleService(repository, coordinatesRepository, validator, eventBus);
    }

    @Test
    void createGeneratesIdAndPublishesEvent() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        VehicleEventBus.Listener listener = mock(VehicleEventBus.Listener.class);
        eventBus.subscribe(listener);

        Vehicle v = vehicle(null, "Quadrocopter", VehicleType.DRONE, FuelType.ELECTRICITY, 5.5);
        Vehicle saved = service.create(v);

        assertNull(saved.getId());
        verify(repository).save(any(Vehicle.class));
        verify(listener).onVehicleChanged(eq(VehicleEventBus.Action.CREATED), any(Vehicle.class));
    }

    @Test
    void createRejectsInvalidVehicle() {
        Vehicle v = vehicle(null, "ok", null, null, 5);
        ReflectionTestUtils.setField(v, "fuelConsumption", -1.0);
        ReflectionTestUtils.setField(v.getCoordinates(), "x", 900.0);
        ValidationException ex = assertThrows(ValidationException.class, () -> service.create(v));
        assertTrue(ex.getFieldErrors().containsKey("fuelConsumption"));
        assertTrue(ex.getFieldErrors().containsKey("coordinates.x"));
    }

    @Test
    void emptyNameRejected() {
        Vehicle v = vehicle(null, "tmp", null, null, 1);
        ReflectionTestUtils.setField(v, "name", "");
        ValidationException ex = assertThrows(ValidationException.class, () -> service.create(v));
        assertTrue(ex.getFieldErrors().containsKey("name"));
    }

    @Test
    void sumFuelConsumptionDelegatesToRepository() {
        when(repository.sumFuelConsumption()).thenReturn(42.0);
        assertEquals(42.0, service.sumFuelConsumption());
    }

    @Test
    void minByTypeReturnsDroneVehicle() {
        when(repository.findAll()).thenReturn(List.of(
                vehicle(1L, "Bike", VehicleType.BICYCLE, null, 1),
                vehicle(2L, "Yacht", VehicleType.SHIP, null, 2),
                vehicle(3L, "Quadro", VehicleType.DRONE, null, 3)
        ));
        Optional<Vehicle> min = service.minByType();
        assertTrue(min.isPresent());
        assertEquals(VehicleType.DRONE, min.get().getType());
    }

    @Test
    void countFuelTypeGreaterThanUsesOrdinalOrder() {
        when(repository.findAll()).thenReturn(List.of(
                vehicle(1L, "A", null, FuelType.GASOLINE, 1),
                vehicle(2L, "B", null, FuelType.NUCLEAR, 1),
                vehicle(3L, "C", null, FuelType.ANTIMATTER, 1),
                vehicle(4L, "D", null, FuelType.ELECTRICITY, 1),
                vehicle(5L, "E", null, null, 1)
        ));
        assertEquals(2, service.countFuelTypeGreaterThan(FuelType.ELECTRICITY));
    }

    @Test
    void findByTypeValidatesInput() {
        assertThrows(ValidationException.class, () -> service.findByType((VehicleType) null));
        when(repository.findByType(VehicleType.BOAT)).thenReturn(List.of(vehicle(1L, "Boat", VehicleType.BOAT, null, 1)));
        assertEquals(1, service.findByType(VehicleType.BOAT).size());
    }

    @Test
    void addWheelsAddsToExistingCount() {
        Vehicle v = vehicle(7L, "Truck", VehicleType.SHIP, null, 3);
        v.setNumberOfWheels(6L);
        when(repository.findById(7L)).thenReturn(Optional.of(v));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Vehicle result = service.addWheels(7L, 4);
        assertEquals(10L, result.getNumberOfWheels());

        ArgumentCaptor<Vehicle> cap = ArgumentCaptor.forClass(Vehicle.class);
        verify(repository).save(cap.capture());
        assertEquals(10L, cap.getValue().getNumberOfWheels());
    }

    @Test
    void addWheelsTreatsNullAsZero() {
        Vehicle v = vehicle(8L, "Sailor", VehicleType.BOAT, null, 3);
        v.setNumberOfWheels(null);
        when(repository.findById(8L)).thenReturn(Optional.of(v));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertEquals(3L, service.addWheels(8L, 3).getNumberOfWheels());
    }

    @Test
    void addWheelsRejectsNonPositiveCount() {
        assertThrows(ValidationException.class, () -> service.addWheels(1L, 0));
    }

    @Test
    void updateKeepsIdAndCreationDate() {
        Vehicle existing = vehicle(5L, "Old", VehicleType.BOAT, FuelType.GASOLINE, 2);
        existing.setCreationDate(java.time.ZonedDateTime.now().minusDays(1));
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Vehicle changes = vehicle(999L, "New", VehicleType.SHIP, FuelType.NUCLEAR, 3);
        Vehicle updated = service.update(5L, changes);

        assertEquals(5L, updated.getId());
        assertNotNull(updated.getCreationDate());
        assertEquals("New", updated.getName());
    }

    @Test
    void deletePublishesEventAndMissingThrows() {
        Vehicle v = vehicle(1L, "X", null, null, 1);
        when(repository.findById(1L)).thenReturn(Optional.of(v));
        service.delete(1L);
        verify(repository).deleteById(1L);
        assertThrows(ResourceNotFoundException.class, () -> service.delete(404L));
    }

    @Test
    void coordinatesLimitsEnforced() {
        assertThrows(IllegalArgumentException.class, () -> new Coordinates(863.0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Coordinates(0.0, -860));
        assertDoesNotThrow(() -> new Coordinates(862.0, -859));
    }
}
