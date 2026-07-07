package sri.microservices.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import sri.microservices.gateway.config.GatewayProperties;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardGatewayController {

    private final RestClient sensoresClient;
    private final RestClient riegoClient;
    private final RestClient reportesClient;

    public DashboardGatewayController(GatewayProperties properties) {
        this.sensoresClient = RestClient.builder().baseUrl(properties.sensoresServiceUrl()).build();
        this.riegoClient = RestClient.builder().baseUrl(properties.riegoServiceUrl()).build();
        this.reportesClient = RestClient.builder().baseUrl(properties.reportesServiceUrl()).build();
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerDashboard() {
        return ResponseEntity.ok(obtenerDashboardSeguro());
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> obtenerDashboardLive() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("dashboard", obtenerDashboardSeguro());
        response.put("estadoRiego", obtenerEstadoRiegoSeguro());
        response.put("telemetria", obtenerTelemetriaSeguro());
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> obtenerDashboardSeguro() {
        Map<String, Object> dashboard = getMap(sensoresClient, "/api/estado-vivo");
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resumen", getMap(reportesClient, "/api/estadisticas/resumen"));
        response.put("mqttActivo", dashboard.get("mqtt_activo"));
        response.put("bombaActiva", dashboard.get("bomba_activa"));
        response.put("humedad", dashboard.get("humedad"));
        response.put("distancia", dashboard.get("distancia"));
        response.put("lecturaTimestamp", dashboard.get("lectura_timestamp"));
        response.put("alerta", dashboard.get("alerta"));
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    private Map<String, Object> obtenerEstadoRiegoSeguro() {
        return getMap(riegoClient, "/api/riego/estado");
    }

    private Object obtenerTelemetriaSeguro() {
        try {
            return reportesClient.get()
                    .uri("/api/estadisticas/telemetria")
                    .retrieve()
                    .body(Object.class);
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(RestClient client, String uri) {
        try {
            Map<String, Object> body = client.get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return body != null ? body : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }
}
