package sri.microservices.sensores.dto;

public record SensorData(
        int humedad,
        double distancia,
        Boolean bombaActiva
) {
}
