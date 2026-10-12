package ru.itmo.vehicle.domain;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Entity
@Table(name = "coordinates")
public class Coordinates {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull(message = "Поле x не может быть null")
    @Max(value = 862, message = "Максимальное значение поля x: 862")
    @Column(name = "x", nullable = false)
    private Double x;

    @Min(value = -859, message = "Значение поля y должно быть больше -860")
    @Column(name = "y", nullable = false)
    private int y;

    public Coordinates() {
    }

    public Coordinates(Double x, int y) {
        setX(x);
        setY(y);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        if (x != null && x > 862) {
            throw new IllegalArgumentException("Максимальное значение поля x: 862");
        }
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        if (y <= -860) {
            throw new IllegalArgumentException("Значение поля y должно быть больше -860");
        }
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinates)) return false;
        Coordinates that = (Coordinates) o;
        return y == that.y && Objects.equals(x, that.x);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
