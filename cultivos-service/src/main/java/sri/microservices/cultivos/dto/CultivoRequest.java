package sri.microservices.cultivos.dto;

public record CultivoRequest(
        String nombre,
        Integer humedadMinOptima,
        Integer humedadMaxOptima,
        Integer duracionRiegoMinutos,
        String tratoRecomendado
) {
}
