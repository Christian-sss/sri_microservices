package sri.microservices.riego.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sri.microservices.riego.model.Cultivo;

import java.util.List;

public interface CultivoRepository extends JpaRepository<Cultivo, Integer> {

    List<Cultivo> findByActivoTrue();
}
