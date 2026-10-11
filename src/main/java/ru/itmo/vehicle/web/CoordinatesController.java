package ru.itmo.vehicle.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.vehicle.domain.Coordinates;
import ru.itmo.vehicle.service.ValidationException;
import ru.itmo.vehicle.service.CoordinatesService;
import ru.itmo.vehicle.service.VehicleService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coordinates")
public class CoordinatesController {
    @Autowired
    private CoordinatesService coordinatesService;

    @GetMapping
    public List<Coordinates> list() {
        return coordinatesService.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Coordinates coordinates) {
        try {
            Coordinates saved = coordinatesService.create(coordinates);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @RequestParam Long replacementId) {
        try {
            service.deleteCoordinates(id, replacementId);
            return ResponseEntity.ok(Map.of("deleted", id, "replacementId", replacementId));
        } catch (VehicleService.NotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("errors", e.getFieldErrors()));
        }
    }

    @Autowired
    private VehicleService service;
}
