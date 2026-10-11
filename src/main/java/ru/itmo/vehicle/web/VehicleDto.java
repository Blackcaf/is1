package ru.itmo.vehicle.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.itmo.vehicle.domain.Coordinates;
import ru.itmo.vehicle.domain.FuelType;
import ru.itmo.vehicle.domain.Vehicle;
import ru.itmo.vehicle.domain.VehicleType;

import java.time.ZonedDateTime;

/**
 * DTO для передачи Vehicle в JSON.
 */
public class VehicleDto {

    public Long id;
    public String name;
    public Coordinates coordinates;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss z")
    public ZonedDateTime creationDate;
    public VehicleType type;
    public double enginePower;
    public Long numberOfWheels;
    public Long capacity;
    public float distanceTravelled;
    public double fuelConsumption;
    public FuelType fuelType;

    public VehicleDto() {
    }

    public static VehicleDto from(Vehicle v) {
        VehicleDto d = new VehicleDto();
        d.id = v.getId();
        d.name = v.getName();
        d.coordinates = v.getCoordinates();
        d.creationDate = v.getCreationDate();
        d.type = v.getType();
        d.enginePower = v.getEnginePower();
        d.numberOfWheels = v.getNumberOfWheels();
        d.capacity = v.getCapacity();
        d.distanceTravelled = v.getDistanceTravelled();
        d.fuelConsumption = v.getFuelConsumption();
        d.fuelType = v.getFuelType();
        return d;
    }

    public Vehicle toEntity() {
        Vehicle v = new Vehicle();
        v.setId(id);
        try {
            v.setName(name);
        } catch (IllegalArgumentException ignored) { // будет обработано валидацией сервиса
        }
        v.setCoordinates(coordinates);
        v.setCreationDate(creationDate);
        v.setType(type);
        v.setEnginePower(enginePower);
        v.setNumberOfWheels(numberOfWheels);
        v.setCapacity(capacity);
        v.setDistanceTravelled(distanceTravelled);
        v.setFuelConsumption(fuelConsumption);
        v.setFuelType(fuelType);
        return v;
    }
}
