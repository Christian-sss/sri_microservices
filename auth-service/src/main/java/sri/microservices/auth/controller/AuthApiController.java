package sri.microservices.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.microservices.auth.dto.LoginRequest;
import sri.microservices.auth.model.User;
import sri.microservices.auth.repository.UsuarioRepository;
import sri.microservices.auth.service.UsuarioService;

import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginEscritorio(@RequestBody LoginRequest request) {
        if (request == null || esBlanco(request.email()) || esBlanco(request.password())) {
            return ResponseEntity.badRequest().body(Map.of("error", "email y password son obligatorios."));
        }

        try {
            User usuario = usuarioService.ejecutar(request.email().trim(), request.password().trim());
            return ResponseEntity.ok(Map.of(
                    "token", "SESSION-TOKEN-" + usuario.getId(),
                    "usuario", toUsuarioResponse(usuario)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas o usuario no encontrado."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> obtenerMisDatos(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        try {
            if (token == null || !token.startsWith("Bearer SESSION-TOKEN-")) {
                throw new IllegalArgumentException("Token invalido.");
            }

            Integer usuarioId = Integer.parseInt(token.replace("Bearer SESSION-TOKEN-", "").trim());
            User usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

            return ResponseEntity.ok(toUsuarioResponse(usuario));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token invalido."));
        }
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
