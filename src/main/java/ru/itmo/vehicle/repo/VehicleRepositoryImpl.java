package ru.itmo.vehicle.repo;

import org.springframework.stereotype.Repository;
import ru.itmo.vehicle.domain.FuelType;
import ru.itmo.vehicle.domain.Vehicle;
import ru.itmo.vehicle.domain.VehicleType;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Repository
public class VehicleRepositoryImpl implements VehicleRepository {
    private static final Map<String, String> SORTABLE_FIELDS = Map.ofEntries(
            Map.entry("id", "v.id"),
            Map.entry("name", "v.name"),
            Map.entry("type", "v.type"),
            Map.entry("fuelType", "v.fuelType"),
            Map.entry("creationDate", "v.creationDate"),
            Map.entry("enginePower", "v.enginePower"),
            Map.entry("numberOfWheels", "v.numberOfWheels"),
            Map.entry("capacity", "v.capacity"),
            Map.entry("distanceTravelled", "v.distanceTravelled"),
            Map.entry("fuelConsumption", "v.fuelConsumption"),
            Map.entry("coordX", "v.coordinates.x"),
            Map.entry("coordY", "v.coordinates.y")
    );

    @PersistenceContext
    private EntityManager em;

    @Override
    public Vehicle save(Vehicle vehicle) {
        if (vehicle.getId() == null) {
            em.persist(vehicle);
            return vehicle;
        }
        return em.merge(vehicle);
    }

    @Override
    public Optional<Vehicle> findById(Long id) {
        return Optional.ofNullable(em.find(Vehicle.class, id));
    }

    @Override
    public List<Vehicle> findAll() {
        return em.createQuery("SELECT v FROM Vehicle v ORDER BY v.id", Vehicle.class).getResultList();
    }

    @Override
    public List<Vehicle> findPage(String filterField, String filterValue, String sortField, boolean asc,
                                  int offset, int limit) {
        StringBuilder jpql = new StringBuilder("SELECT v FROM Vehicle v");
        appendFilter(jpql, filterField, filterValue);
        appendSort(jpql, sortField, asc);
        TypedQuery<Vehicle> q = em.createQuery(jpql.toString(), Vehicle.class);
        if (hasFilter(filterField, filterValue)) {
            q.setParameter("filterVal", "%" + filterValue.toLowerCase(Locale.ROOT) + "%");
        }
        q.setFirstResult(offset).setMaxResults(limit);
        return q.getResultList();
    }

    @Override
    public long countFiltered(String filterField, String filterValue) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(v) FROM Vehicle v");
        appendFilter(jpql, filterField, filterValue);
        TypedQuery<Long> q = em.createQuery(jpql.toString(), Long.class);
        if (hasFilter(filterField, filterValue)) {
            q.setParameter("filterVal", "%" + filterValue.toLowerCase(Locale.ROOT) + "%");
        }
        return q.getSingleResult();
    }

    @Override
    public void deleteById(Long id) {
        Vehicle v = em.find(Vehicle.class, id);
        if (v != null) {
            em.remove(v);
        }
    }

    private boolean hasFilter(String field, String value) {
        return field != null && value != null && !value.isBlank() && SORTABLE_FIELDS.containsKey(field);
    }

    private void appendFilter(StringBuilder jpql, String field, String value) {
        if (hasFilter(field, value)) {
            String col = SORTABLE_FIELDS.get(field);
            if ("v.name".equals(col)) {
                jpql.append(" WHERE LOWER(v.name) LIKE :filterVal");
            } else if ("v.type".equals(col) || "v.fuelType".equals(col)) {
                jpql.append(" WHERE LOWER(CAST(").append(col).append(" AS STRING)) LIKE :filterVal");
            } else {
                jpql.append(" WHERE CAST(").append(col).append(" AS STRING) LIKE :filterVal");
            }
        }
    }

    private void appendSort(StringBuilder jpql, String field, boolean asc) {
        String col = (field != null && SORTABLE_FIELDS.containsKey(field)) ? SORTABLE_FIELDS.get(field) : "v.id";
        jpql.append(" ORDER BY ").append(col).append(asc ? " ASC" : " DESC");
    }

    @Override
    public double sumFuelConsumption() {
        Double res = em.createQuery("SELECT COALESCE(SUM(v.fuelConsumption), 0.0) FROM Vehicle v", Double.class)
                .getSingleResult();
        return res == null ? 0.0 : res;
    }

    @Override
    public Optional<Vehicle> minByType() {
        List<Vehicle> list = em.createQuery(
                "SELECT v FROM Vehicle v WHERE v.type IS NOT NULL ORDER BY v.type ASC", Vehicle.class)
                .setMaxResults(1).getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public long countByFuelTypeGreaterThan(FuelType fuelType) {
        return em.createQuery("SELECT COUNT(v) FROM Vehicle v WHERE v.fuelType IS NOT NULL AND v.fuelType > :ft", Long.class)
                .setParameter("ft", fuelType)
                .getSingleResult();
    }

    @Override
    public List<Vehicle> findByType(VehicleType type) {
        return em.createQuery("SELECT v FROM Vehicle v WHERE v.type = :t ORDER BY v.id", Vehicle.class)
                .setParameter("t", type)
                .getResultList();
    }

    @Override
    public List<Vehicle> findByCoordinatesId(Long coordinatesId) {
        return em.createQuery("SELECT v FROM Vehicle v WHERE v.coordinates.id = :id", Vehicle.class)
                .setParameter("id", coordinatesId)
                .getResultList();
    }
}
