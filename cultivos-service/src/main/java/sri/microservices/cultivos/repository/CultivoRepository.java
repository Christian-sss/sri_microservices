package sri.microservices.cultivos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sri.microservices.cultivos.model.Cultivo;

import java.util.List;

public interface CultivoRepository extends JpaRepository<Cultivo, Integer> {

    List<Cultivo> findByActivoTrue();

    List<Cultivo> findByActivoFalse();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
}
