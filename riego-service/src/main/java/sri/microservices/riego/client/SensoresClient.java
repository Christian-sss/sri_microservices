package sri.microservices.riego.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sri.microservices.riego.config.ServiceProperties;
import sri.microservices.riego.dto.SensorData;

import java.util.Map;

@Component
public class SensoresClient {

    private final RestClient restClient;

    public SensoresClient(ServiceProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.sensoresUrl())
                .build();
    }

    public SensorData obtenerUltimaLectura() {
        return restClient.get()
                .uri("/api/sensor/ultima-lectura")
                .retrieve()
                .body(SensorData.class);
    }

    public void enviarComando(int comando) {
        restClient.post()
                .uri("/api/sensor/comando")
                .body(Map.of("comando", comando))
                .retrieve()
                .toBodilessEntity();
    }
}
