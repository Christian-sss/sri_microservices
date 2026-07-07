package sri.microservices.riego.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
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
        try {
            return restClient.get()
                    .uri("/api/sensor/ultima-lectura")
                    .retrieve()
                    .body(SensorData.class);
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("No se pudo obtener la ultima lectura del sensor: "
                    + obtenerDetalleError(exception), exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("No se pudo conectar con sensores-service.", exception);
        }
    }

    public void enviarComando(int comando) {
        try {
            restClient.post()
                    .uri("/api/sensor/comando")
                    .body(Map.of("comando", comando))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("No se pudo enviar el comando de riego: "
                    + obtenerDetalleError(exception), exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("No se pudo conectar con sensores-service.", exception);
        }
    }

    private String obtenerDetalleError(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            return body;
        }
        return "HTTP " + exception.getStatusCode().value();
    }
}
