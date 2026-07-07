package sri.microservices.reportes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sri.microservices.reportes.model.ConfiguracionRiego;

public interface ConfiguracionRiegoRepository extends JpaRepository<ConfiguracionRiego, Integer> {
}
