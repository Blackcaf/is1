package ru.itmo.vehicle.service;

import org.springframework.stereotype.Service;
import ru.itmo.vehicle.domain.Vehicle;

/**
 * Шина событий: позволяет интерфейсам (и другим клиентам) автоматически
 * узнавать об изменениях объектов (добавление/изменение/удаление).
 */
@Service
public class VehicleEventBus {

    public enum Action { CREATED, UPDATED, DELETED }

    public interface Listener {
        void onVehicleChanged(Action action, Vehicle vehicle);
    }

    private final java.util.List<Listener> listeners =
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    public void subscribe(Listener l) {
        listeners.add(l);
    }

    public void unsubscribe(Listener l) {
        listeners.remove(l);
    }

    public void publish(Action action, Vehicle vehicle) {
        for (Listener l : new java.util.ArrayList<>(listeners)) {
            try {
                l.onVehicleChanged(action, vehicle);
            } catch (Exception ignored) {
                // подписчик не должен ронять бизнес-операцию
            }
        }
    }
}
