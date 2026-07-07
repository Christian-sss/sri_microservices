package sri.microservices.riego.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sri.microservices.riego.model.EventoRiego;
import sri.microservices.riego.model.enums.EstadoRiego;
import sri.microservices.riego.model.enums.ModoRiego;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EventoRiegoRepository extends JpaRepository<EventoRiego, Long> {

    Optional<EventoRiego> findFirstByEstadoOrderByFechaInicioDesc(EstadoRiego estado);

    long countByModoRiegoAndFechaInicioBetween(ModoRiego modoRiego,
                                               LocalDateTime inicio,
                                               LocalDateTime fin);
}
