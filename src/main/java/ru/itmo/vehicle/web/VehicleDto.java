package ru.itmo.vehicle.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.itmo.vehicle.domain.Coordinates;
import ru.itmo.vehicle.domain.FuelType;
import ru.itmo.vehicle.domain.VehicleType;

import java.time.ZonedDateTime;

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
}
