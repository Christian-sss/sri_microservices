package sri.microservices.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sri.microservices.auth.model.User;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);
}
