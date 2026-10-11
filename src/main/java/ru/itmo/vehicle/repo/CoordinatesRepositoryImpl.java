package ru.itmo.vehicle.repo;

import org.springframework.stereotype.Repository;
import ru.itmo.vehicle.domain.Coordinates;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
public class CoordinatesRepositoryImpl implements CoordinatesRepository {
    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Coordinates> findAll() {
        return em.createQuery("SELECT c FROM Coordinates c ORDER BY c.id", Coordinates.class)
                .getResultList();
    }

    @Override
    public Optional<Coordinates> findById(Long id) {
        return Optional.ofNullable(em.find(Coordinates.class, id));
    }

    @Override
    public Coordinates save(Coordinates coordinates) {
        if (coordinates.getId() == null) {
            em.persist(coordinates);
            return coordinates;
        }
        return em.merge(coordinates);
    }

    @Override
    public void delete(Coordinates coordinates) {
        em.remove(em.contains(coordinates) ? coordinates : em.merge(coordinates));
    }
}
