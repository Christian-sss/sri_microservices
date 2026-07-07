package sri.microservices.reportes.dto;

public record TelemetriaResponse(
        String fechaLectura,
        String etiqueta,
        Integer humedad,
        Double distancia
) {
}
