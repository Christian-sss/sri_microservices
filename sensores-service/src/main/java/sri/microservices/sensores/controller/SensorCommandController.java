package sri.microservices.sensores.controller;

import lombok.RequiredArgsConstructor;
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
}
