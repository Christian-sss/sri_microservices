package sri.microservices.sensores.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.microservices.sensores.dto.SensorData;
import sri.microservices.sensores.integration.Esp32MqttConnectionManager;
import sri.microservices.sensores.integration.Esp32MqttControlRiego;
import sri.microservices.sensores.integration.Esp32MqttSensor;
import sri.microservices.sensores.service.AlertaRiegoService;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class EstadoVivoController {

    private final Esp32MqttConnectionManager mqttConnectionManager;
    private final Esp32MqttControlRiego mqttControlRiego;
    private final Esp32MqttSensor mqttSensor;
    private final AlertaRiegoService alertaRiegoService;

    @GetMapping("/api/estado-vivo")
    public Map<String, Object> obtenerEstadoVivo() {
        Map<String, Object> estado = new LinkedHashMap<>();
        SensorData ultimaLectura = mqttSensor.getUltimoDato();
        boolean bombaActiva = ultimaLectura != null && ultimaLectura.bombaActiva() != null
                ? ultimaLectura.bombaActiva()
                : mqttControlRiego.isBombaActiva();

        estado.put("mqtt_activo", mqttConnectionManager.estaConectado());
        estado.put("bomba_activa", bombaActiva);
        estado.put("humedad", ultimaLectura != null ? ultimaLectura.humedad() : null);
        estado.put("distancia", ultimaLectura != null ? ultimaLectura.distancia() : null);
        estado.put("lectura_timestamp", mqttSensor.getUltimaLecturaEn());
        estado.put("alerta", alertaRiegoService.consumirUltimaAlerta());
        estado.put("timestamp", LocalDateTime.now().toString());
        return estado;
    }

    @GetMapping("/api/mqtt/status")
    public Map<String, Boolean> obtenerEstadoMqtt() {
        return Map.of("conectado", mqttConnectionManager.estaConectado());
    }
}
