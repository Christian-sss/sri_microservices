package sri.microservices.reportes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sri.microservices.reportes.model.Cultivo;

public interface CultivoRepository extends JpaRepository<Cultivo, Integer> {
}
