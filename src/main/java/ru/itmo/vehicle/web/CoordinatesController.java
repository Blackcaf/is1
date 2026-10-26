package ru.itmo.vehicle.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itmo.vehicle.domain.Coordinates;
import ru.itmo.vehicle.service.CoordinatesService;
import ru.itmo.vehicle.service.VehicleService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coordinates")
public class CoordinatesController {
    private final CoordinatesService coordinatesService;
    private final VehicleService vehicleService;

    public CoordinatesController(CoordinatesService coordinatesService, VehicleService vehicleService) {
        this.coordinatesService = coordinatesService;
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<Coordinates> list() { return coordinatesService.findAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Coordinates create(@RequestBody Coordinates coordinates) { return coordinatesService.create(coordinates); }

    @DeleteMapping("/{id}")
    public Map<String, Long> delete(@PathVariable Long id, @RequestParam Long replacementId) {
        vehicleService.deleteCoordinates(id, replacementId);
        return Map.of("deleted", id, "replacementId", replacementId);
    }
}
