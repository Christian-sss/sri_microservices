package sri.microservices.riego.dto;

public record RiegoEstadoResponse(
        String modo,
        boolean automatico,
        Integer cultivoActivoId,
        String cultivoActivoNombre,
        String horaRiegoProgramada,
        String mensaje
) {
}
