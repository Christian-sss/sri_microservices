package sri.microservices.auth.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.microservices.auth.dto.AuthRequest;
import sri.microservices.auth.dto.GoogleAuthRequest;
import sri.microservices.auth.model.User;
import sri.microservices.auth.service.UsuarioService;

import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> ingresar(@RequestBody AuthRequest request, HttpSession session) {
        if (request == null || esBlanco(request.username()) || esBlanco(request.password())) {
            return ResponseEntity.badRequest().body(Map.of("error", "username y password son obligatorios."));
        }

        try {
            User usuario = usuarioService.ejecutar(request.username().trim(), request.password().trim());
            session.setAttribute("usuarioLogueado", usuario);
            return ResponseEntity.ok(Map.of("mensaje", "Login correcto.", "usuario", toUsuarioResponse(usuario)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario o contrasena incorrectos."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registrar(@RequestBody AuthRequest request) {
        if (request == null || esBlanco(request.username()) || esBlanco(request.password())) {
            return ResponseEntity.badRequest().body(Map.of("error", "username y password son obligatorios."));
        }

        try {
            User usuario = usuarioService.registrar(request.username().trim(), request.password().trim());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("mensaje", "Registro exitoso.", "usuario", toUsuarioResponse(usuario)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> ingresarConGoogle(@RequestBody GoogleAuthRequest request, HttpSession session) {
        if (request == null || esBlanco(request.credential())) {
            return ResponseEntity.badRequest().body(Map.of("error", "credential es obligatorio."));
        }

        try {
            User usuario = usuarioService.autenticarConGoogle(request.credential());
            session.setAttribute("usuarioLogueado", usuario);
            return ResponseEntity.ok(Map.of("mensaje", "Login con Google correcto.", "usuario", toUsuarioResponse(usuario)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Error autenticando con Google."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> usuarioActual(HttpSession session) {
        User usuario = (User) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No hay una sesion activa."));
        }

        return ResponseEntity.ok(Map.of("usuario", toUsuarioResponse(usuario)));
    }

    @PostMapping("/logout")
    public Map<String, String> salir(HttpSession session) {
        session.invalidate();
        return Map.of("mensaje", "Sesion cerrada correctamente.");
    }

    private Map<String, Object> toUsuarioResponse(User usuario) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", usuario.getId());
        response.put("nombre", usuario.getNombre());
        response.put("email", usuario.getEmail());
        response.put("pictureUrl", usuario.getPictureUrl());
        response.put("fechaCreacion", usuario.getFechaCreacion());
        return response;
    }

    private boolean esBlanco(String valor) {
        return valor == null || valor.isBlank();
    }
}
