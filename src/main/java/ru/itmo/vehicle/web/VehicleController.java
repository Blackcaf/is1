package ru.itmo.vehicle.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.itmo.vehicle.domain.Vehicle;
import ru.itmo.vehicle.service.VehicleEventBus;
import ru.itmo.vehicle.service.VehicleService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleService service;
    private final VehicleMapper mapper;
    private final VehicleEventBus eventBus;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public VehicleController(VehicleService service, VehicleMapper mapper, VehicleEventBus eventBus) {
        this.service = service;
        this.mapper = mapper;
        this.eventBus = eventBus;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleDto create(@RequestBody VehicleDto dto) {
        return mapper.toDto(service.create(mapper.toEntity(dto)));
    }

    @GetMapping("/{id}")
    public VehicleDto get(@PathVariable Long id) {
        return mapper.toDto(service.getRequired(id));
    }

    @PutMapping("/{id}")
    public VehicleDto update(@PathVariable Long id, @RequestBody VehicleDto dto) {
        return mapper.toDto(service.update(id, mapper.toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public Map<String, Long> delete(@PathVariable Long id) {
        service.delete(id);
        return Map.of("deleted", id);
    }

    @GetMapping
    public VehiclePageDto list(@RequestParam(required = false) String filterField,
                               @RequestParam(required = false) String filterValue,
                               @RequestParam(required = false) String sortField,
                               @RequestParam(defaultValue = "true") boolean asc,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        List<VehicleDto> items = service.page(filterField, filterValue, sortField, asc, page, size)
                .stream().map(mapper::toDto).toList();
        return new VehiclePageDto(items, service.count(filterField, filterValue), page, size);
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        SseEmitter emitter = new SseEmitter(0L);
        VehicleEventBus.Listener listener = (action, vehicle) -> sendEvent(emitter, action, vehicle);
        Runnable cleanup = () -> { emitters.remove(emitter); eventBus.unsubscribe(listener); };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());
        emitters.add(emitter);
        eventBus.subscribe(listener);
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException | IllegalStateException exception) {
            cleanup.run();
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    private void sendEvent(SseEmitter emitter, VehicleEventBus.Action action, Vehicle vehicle) {
        try {
            emitter.send(SseEmitter.event().name(action.name().toLowerCase())
                    .data(Map.of("vehicle", mapper.toDto(vehicle))));
        } catch (IOException | IllegalStateException exception) {
            emitter.completeWithError(exception);
        }
    }
}
