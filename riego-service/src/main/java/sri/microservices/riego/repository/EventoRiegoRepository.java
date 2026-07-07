package sri.microservices.riego.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sri.microservices.riego.model.EventoRiego;
import sri.microservices.riego.model.enums.EstadoRiego;
import sri.microservices.riego.model.enums.ModoRiego;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EventoRiegoRepository extends JpaRepository<EventoRiego, Long> {

    Optional<EventoRiego> findFirstByEstadoOrderByFechaInicioDesc(EstadoRiego estado);

<<<<<<< HEAD
    boolean existsByModoRiegoAndFechaInicioBetween(
            ModoRiego modoRiego,
            LocalDateTime inicio,
            LocalDateTime fin
    );
=======
    long countByModoRiegoAndFechaInicioBetween(ModoRiego modoRiego,
                                               LocalDateTime inicio,
                                               LocalDateTime fin);
>>>>>>> 16018ee81f76f239fc266973c5a87e9d6d395446
}
