package sri.microservices.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sri.microservices.auth.model.User;
import sri.microservices.auth.repository.UsuarioRepository;

import java.util.Collections;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final String googleClientId;

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            @Value("${google.client-id:}") String googleClientId
    ) {
        this.usuarioRepository = usuarioRepository;
        this.googleClientId = googleClientId;
    }

    @Override
    public User registrar(String email, String password) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("El correo ya esta registrado.");
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setNombre(email.split("@")[0]);
        newUser.setPasswordHash(password);
        newUser.setPictureUrl("");
        return usuarioRepository.save(newUser);
    }

    @Override
    public User ejecutar(String email, String passwordIngresada) {
        var user = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (!passwordIngresada.equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("Contrasena incorrecta.");
        }

        return user;
    }

    @Override
    public User autenticarConGoogle(String idTokenString) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_ID no configurado.");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new IllegalArgumentException("Token de Google invalido.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            return usuarioRepository.findByEmail(email).map(user -> {
                user.setPictureUrl(pictureUrl);
                return usuarioRepository.save(user);
            }).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setNombre(name);
                newUser.setPasswordHash("");
                newUser.setPictureUrl(pictureUrl);
                return usuarioRepository.save(newUser);
            });
        } catch (Exception e) {
            throw new RuntimeException("Error verificando token de Google", e);
        }
    }
}
