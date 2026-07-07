package sri.microservices.reportes.dto;

public record ConsumoAguaDetalleResponse(
        String fecha,
        String cultivo,
        String horaInicio,
        String horaFin,
        double litrosConsumidos
) {
}
