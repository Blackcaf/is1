package ru.itmo.vehicle.web;

import java.util.List;

public record VehiclePageDto(List<VehicleDto> items, long total, int page, int size) { }
