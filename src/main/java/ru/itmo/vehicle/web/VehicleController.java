package ru.itmo.vehicle.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.itmo.vehicle.domain.Vehicle;
import ru.itmo.vehicle.service.ValidationException;
import ru.itmo.vehicle.service.VehicleEventBus;
import ru.itmo.vehicle.service.VehicleService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * REST API для операций CRUD над Vehicle + SSE-канал для мгновенной
 * синхронизации изменений между всеми клиентами.
 */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService service;

    @Autowired
    private VehicleEventBus eventBus;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final AtomicLong sseId = new AtomicLong();

    /* ------------------------- CRUD ------------------------- */

    @PostMapping
    public ResponseEntity<?> create(@RequestBody VehicleDto dto) {
        try {
            Vehicle v = service.create(dto.toEntity());
            return ResponseEntity.status(HttpStatus.CREATED).body(VehicleDto.from(v));
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("errors", e.getFieldErrors()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("errors", Map.of("general", e.getMessage())));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return service.getById(id)
                .map(v -> ResponseEntity.ok((Object) VehicleDto.from(v)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Транспортное средство с id=" + id + " не найдено")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody VehicleDto dto) {
        try {
            Vehicle v = service.update(id, dto.toEntity());
            return ResponseEntity.ok(VehicleDto.from(v));
        } catch (VehicleService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("errors", e.getFieldErrors()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("errors", Map.of("general", e.getMessage())));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(Map.of("deleted", id));
        } catch (VehicleService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /* ------------------------- таблица (пагинация/фильтр/сортировка) ------------------------- */

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String filterField,
                                    @RequestParam(required = false) String filterValue,
                                    @RequestParam(required = false) String sortField,
                                    @RequestParam(defaultValue = "true") boolean asc,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        List<VehicleDto> items = service.page(filterField, filterValue, sortField, asc, page, size)
                .stream().map(VehicleDto::from).toList();
        long total = service.count(filterField, filterValue);
        return Map.of("items", items, "total", total, "page", page, "size", size);
    }

    /* ------------------------- SSE: автообновление у всех клиентов ------------------------- */

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        SseEmitter emitter = new SseEmitter(0L); // без таймаута
        VehicleEventBus.Listener listener = (action, vehicle) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(action.name().toLowerCase())
                        .data(Map.of("vehicle", VehicleDto.from(vehicle))));
            } catch (IOException | IllegalStateException ex) {
                emitter.completeWithError(ex);
            }
        };
        Runnable cleanup = () -> {
            emitters.remove(emitter);
            eventBus.unsubscribe(listener);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        emitters.add(emitter);
        eventBus.subscribe(listener);
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException | IllegalStateException ex) {
            cleanup.run();
            emitter.completeWithError(ex);
        }
        return emitter;
    }
}
