package ru.itmo.vehicle.web;

import org.springframework.web.bind.annotation.*;
import ru.itmo.vehicle.service.VehicleService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/special")
public class SpecialOperationsController {
    private final VehicleService service;
    private final VehicleMapper mapper;

    public SpecialOperationsController(VehicleService service, VehicleMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/fuel-consumption-sum")
    public Map<String, Double> sumFuelConsumption() { return Map.of("result", service.sumFuelConsumption()); }

    @GetMapping("/min-by-type")
    public VehicleDto minByType() { return mapper.toDto(service.minByTypeRequired()); }

    @GetMapping("/count-fuel-type-greater")
    public Map<String, Long> countFuelTypeGreaterThan(@RequestParam String fuelType) {
        return Map.of("result", service.countFuelTypeGreaterThan(fuelType));
    }

    @GetMapping("/by-type")
    public Map<String, List<VehicleDto>> byType(@RequestParam String type) {
        return Map.of("items", service.findByType(type).stream().map(mapper::toDto).toList());
    }

    @PostMapping("/{id}/wheels")
    public VehicleDto addWheels(@PathVariable Long id, @RequestParam long count) {
        return mapper.toDto(service.addWheels(id, count));
    }
}
