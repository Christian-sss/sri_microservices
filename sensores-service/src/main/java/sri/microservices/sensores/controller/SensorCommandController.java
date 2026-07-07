package sri.microservices.sensores.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.microservices.sensores.integration.Esp32MqttControlRiego;

import java.util.Map;

@RestController
@RequestMapping("/api/sensor")
@RequiredArgsConstructor
public class SensorCommandController {

    private final Esp32MqttControlRiego controlRiego;

    @PostMapping("/comando")
    public Map<String, Object> enviarComando(@RequestBody ComandoRequest request) {
        if (request == null || request.comando() == null) {
            throw new IllegalArgumentException("El comando es obligatorio.");
        }

        controlRiego.enviarComando(request.comando());
        return Map.of(
                "mensaje", "Comando enviado correctamente.",
                "comando", request.comando()
        );
    }

    private record ComandoRequest(Integer comando) {
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> manejarServicioNoDisponible(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", exception.getMessage()));
    }
}
