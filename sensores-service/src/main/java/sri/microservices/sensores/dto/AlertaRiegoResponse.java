package sri.microservices.sensores.dto;

public record AlertaRiegoResponse(
        Long id,
        String tipo,
        String titulo,
        String mensaje,
        String fecha
) {
}
