package sri.microservices.reportes.dto;

public record DistribucionModosResponse(
        long manual,
        long automatico,
        long total,
        String cultivoActivo
) {
}
