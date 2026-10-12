package ru.itmo.vehicle.domain;

import javax.persistence.*;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.time.ZonedDateTime;

@Entity
@Table(name = "vehicle",
        uniqueConstraints = @UniqueConstraint(columnNames = "id"),
        indexes = @Index(name = "idx_vehicle_name", columnList = "name"))
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 500)
    @NotNull(message = "Поле name не может быть null")
    @NotBlank(message = "Строка name не может быть пустой")
    private String name;

    @ManyToOne(fetch = FetchType.EAGER, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "coordinates_id", nullable = false)
    @NotNull(message = "Поле coordinates не может быть null")
    @Valid
    private Coordinates coordinates;

    @Column(name = "creation_date", nullable = false, updatable = false)
    @Convert(converter = ZonedDateTimeConverter.class)
    @NotNull(message = "Поле creationDate не может быть null")
    private ZonedDateTime creationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private VehicleType type;

    @Column(name = "engine_power", nullable = false)
    @DecimalMin(value = "0.0", inclusive = false, message = "Значение поля enginePower должно быть больше 0")
    private double enginePower;

    @Column(name = "number_of_wheels")
    @Positive(message = "Значение поля numberOfWheels должно быть больше 0")
    private Long numberOfWheels;

    @Column(name = "capacity", nullable = false)
    @NotNull(message = "Поле capacity не может быть null")
    @Positive(message = "Значение поля capacity должно быть больше 0")
    private Long capacity;

    @Column(name = "distance_travelled", nullable = false)
    @DecimalMin(value = "0.0", inclusive = false, message = "Значение поля distanceTravelled должно быть больше 0")
    private float distanceTravelled;

    @Column(name = "fuel_consumption", nullable = false)
    @DecimalMin(value = "0.0", inclusive = false, message = "Значение поля fuelConsumption должно быть больше 0")
    private double fuelConsumption;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", length = 20)
    private FuelType fuelType;

    public Vehicle() {
    }

    @PrePersist
    void onCreate() {
        if (creationDate == null) {
            creationDate = ZonedDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Строка name не может быть пустой");
        }
        this.name = name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(ZonedDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public double getEnginePower() {
        return enginePower;
    }

    public void setEnginePower(double enginePower) {
        if (enginePower <= 0) {
            throw new IllegalArgumentException("Значение поля enginePower должно быть больше 0");
        }
        this.enginePower = enginePower;
    }

    public Long getNumberOfWheels() {
        return numberOfWheels;
    }

    public void setNumberOfWheels(Long numberOfWheels) {
        if (numberOfWheels != null && numberOfWheels <= 0) {
            throw new IllegalArgumentException("Значение поля numberOfWheels должно быть больше 0");
        }
        this.numberOfWheels = numberOfWheels;
    }

    public Long getCapacity() {
        return capacity;
    }

    public void setCapacity(Long capacity) {
        if (capacity != null && capacity <= 0) {
            throw new IllegalArgumentException("Значение поля capacity должно быть больше 0");
        }
        this.capacity = capacity;
    }

    public float getDistanceTravelled() {
        return distanceTravelled;
    }

    public void setDistanceTravelled(float distanceTravelled) {
        if (distanceTravelled <= 0) {
            throw new IllegalArgumentException("Значение поля distanceTravelled должно быть больше 0");
        }
        this.distanceTravelled = distanceTravelled;
    }

    public double getFuelConsumption() {
        return fuelConsumption;
    }

    public void setFuelConsumption(double fuelConsumption) {
        if (fuelConsumption <= 0) {
            throw new IllegalArgumentException("Значение поля fuelConsumption должно быть больше 0");
        }
        this.fuelConsumption = fuelConsumption;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    @Override
    public String toString() {
        return "Vehicle{id=" + id + ", name='" + name + "', type=" + type + "}";
    }
}
