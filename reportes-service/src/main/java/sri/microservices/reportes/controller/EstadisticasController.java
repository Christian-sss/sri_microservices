package sri.microservices.reportes.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sri.microservices.reportes.dto.ConsumoAguaDetalleResponse;
import sri.microservices.reportes.dto.DistribucionModosResponse;
import sri.microservices.reportes.dto.EstadisticasResumenResponse;
import sri.microservices.reportes.dto.TelemetriaResponse;
import sri.microservices.reportes.service.EstadisticasService;

import java.util.List;

@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    @GetMapping("/resumen")
    public EstadisticasResumenResponse obtenerResumen() {
        return estadisticasService.obtenerResumen();
    }

    @GetMapping("/telemetria")
    public List<TelemetriaResponse> obtenerTelemetria(@RequestParam(required = false) String cultivoId) {
        FiltroCultivo filtro = resolverFiltroCultivo(cultivoId);
        return estadisticasService.obtenerTelemetriaReciente(filtro.cultivoId(), filtro.soloMantenimiento());
    }

    @GetMapping("/distribucion-modos")
    public DistribucionModosResponse obtenerDistribucionModos(@RequestParam(required = false) String cultivoId) {
        FiltroCultivo filtro = resolverFiltroCultivo(cultivoId);
        return estadisticasService.obtenerDistribucionModosMesActual(filtro.cultivoId(), filtro.soloMantenimiento());
    }

    @GetMapping("/consumo-detalle")
    public List<ConsumoAguaDetalleResponse> obtenerConsumoAguaDetalle(@RequestParam(required = false) String cultivoId) {
        FiltroCultivo filtro = resolverFiltroCultivo(cultivoId);
        return estadisticasService.obtenerConsumoAguaDetalle(filtro.cultivoId(), filtro.soloMantenimiento());
    }

    private FiltroCultivo resolverFiltroCultivo(String cultivoId) {
        if (cultivoId == null || cultivoId.isBlank()) {
            return new FiltroCultivo(null, false);
        }
        if ("null_value".equalsIgnoreCase(cultivoId.trim())) {
            return new FiltroCultivo(null, true);
        }
        try {
            return new FiltroCultivo(Integer.valueOf(cultivoId), false);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cultivoId debe ser numerico o null_value.");
        }
    }

    private record FiltroCultivo(Integer cultivoId, boolean soloMantenimiento) {
    }
}
