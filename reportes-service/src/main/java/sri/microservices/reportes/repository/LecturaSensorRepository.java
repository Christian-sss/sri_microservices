package sri.microservices.reportes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sri.microservices.reportes.model.LecturaSensor;

import java.util.List;

public interface LecturaSensorRepository extends JpaRepository<LecturaSensor, Long> {

    List<LecturaSensor> findTop20ByOrderByFechaLecturaDesc();

    List<LecturaSensor> findTop20ByCultivoIdOrderByFechaLecturaDesc(Integer cultivoId);

    List<LecturaSensor> findTop20ByCultivoIdIsNullOrderByFechaLecturaDesc();
}
