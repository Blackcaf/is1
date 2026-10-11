package ru.itmo.vehicle.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.vehicle.domain.FuelType;
import ru.itmo.vehicle.domain.VehicleType;
import ru.itmo.vehicle.service.ValidationException;
import ru.itmo.vehicle.service.VehicleService;

import java.util.List;
import java.util.Map;

/**
 * REST API специальных операций бизнес-логики.
 */
@RestController
@RequestMapping("/api/special")
public class SpecialOperationsController {

    @Autowired
    private VehicleService service;

    /** 1. Сумма значений поля fuelConsumption для всех объектов. */
    @GetMapping("/fuel-consumption-sum")
    public Map<String, Object> sumFuelConsumption() {
        return Map.of("result", service.sumFuelConsumption());
    }

    /** 2. Один (любой) объект с минимальным значением поля type. */
    @GetMapping("/min-by-type")
    public ResponseEntity<?> minByType() {
        return service.minByType()
                .map(v -> ResponseEntity.ok((Object) VehicleDto.from(v)))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("error", "Объектов с заполненным полем type не найдено")));
    }

    /** 3. Количество объектов, у которых fuelType больше заданного. */
    @GetMapping("/count-fuel-type-greater")
    public ResponseEntity<?> countFuelTypeGreaterThan(@RequestParam String fuelType) {
        try {
            FuelType ft = FuelType.valueOf(fuelType.toUpperCase());
            return ResponseEntity.ok(Map.of("result", service.countFuelTypeGreaterThan(ft)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("errors",
                    Map.of("fuelType", "Недопустимый тип топлива: " + fuelType
                            + ". Допустимые значения: GASOLINE, KEROSENE, ELECTRICITY, NUCLEAR, ANTIMATTER")));
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("errors", e.getFieldErrors()));
        }
    }

    /** 4. Все транспортные средства заданного типа. */
    @GetMapping("/by-type")
    public ResponseEntity<?> byType(@RequestParam String type) {
        try {
            VehicleType vt = VehicleType.valueOf(type.toUpperCase());
            List<VehicleDto> items = service.findByType(vt).stream().map(VehicleDto::from).toList();
            return ResponseEntity.ok(Map.of("items", items));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("errors",
                    Map.of("type", "Недопустимый тип ТС: " + type
                            + ". Допустимые значения: DRONE, BOAT, SHIP, BICYCLE")));
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("errors", e.getFieldErrors()));
        }
    }

    /** 5. Добавить транспортному средству с заданным id указанное число колёс. */
    @PostMapping("/{id}/wheels")
    public ResponseEntity<?> addWheels(@PathVariable Long id, @RequestParam long count) {
        try {
            var v = service.addWheels(id, count);
            return ResponseEntity.ok(VehicleDto.from(v));
        } catch (VehicleService.NotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("errors", e.getFieldErrors()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("errors", Map.of("general", e.getMessage())));
        }
    }
}
