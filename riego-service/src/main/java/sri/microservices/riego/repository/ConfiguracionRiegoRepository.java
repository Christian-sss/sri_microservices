package sri.microservices.riego.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sri.microservices.riego.model.ConfiguracionRiego;

public interface ConfiguracionRiegoRepository extends JpaRepository<ConfiguracionRiego, Integer> {
}
