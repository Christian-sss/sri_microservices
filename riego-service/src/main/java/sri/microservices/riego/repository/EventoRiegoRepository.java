package sri.microservices.riego.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sri.microservices.riego.model.EventoRiego;
import sri.microservices.riego.model.enums.EstadoRiego;

import java.util.Optional;

public interface EventoRiegoRepository extends JpaRepository<EventoRiego, Long> {

    Optional<EventoRiego> findFirstByEstadoOrderByFechaInicioDesc(EstadoRiego estado);
}
