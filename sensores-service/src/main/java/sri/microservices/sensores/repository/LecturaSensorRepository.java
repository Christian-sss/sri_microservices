package sri.microservices.sensores.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sri.microservices.sensores.model.LecturaSensor;

import java.util.List;

public interface LecturaSensorRepository extends JpaRepository<LecturaSensor, Long> {

    List<LecturaSensor> findTop20ByOrderByFechaLecturaDesc();
}
