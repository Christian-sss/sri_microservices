package sri.microservices.auth.service;

import sri.microservices.auth.model.User;

public interface UsuarioService {

    User ejecutar(String email, String passwordIngresada);

    User registrar(String email, String password);

    User autenticarConGoogle(String idTokenString);
}
