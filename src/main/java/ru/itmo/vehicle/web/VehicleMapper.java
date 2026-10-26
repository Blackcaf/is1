package ru.itmo.vehicle.web;

import org.mapstruct.Mapper;
import ru.itmo.vehicle.domain.Vehicle;

@Mapper(componentModel = "spring")
public interface VehicleMapper {
    VehicleDto toDto(Vehicle vehicle);
    Vehicle toEntity(VehicleDto dto);
}
