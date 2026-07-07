package sri.microservices.riego.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.microservices.riego.dto.CultivoResponse;
import sri.microservices.riego.dto.RiegoEstadoResponse;
import sri.microservices.riego.dto.RiegoManualRequest;
import sri.microservices.riego.dto.RiegoModoRequest;
import sri.microservices.riego.dto.RiegoPerfilRequest;
import sri.microservices.riego.dto.RiegoProgramacionRequest;
import sri.microservices.riego.model.enums.ModoOperacion;
import sri.microservices.riego.service.RiegoControlService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/riego")
@RequiredArgsConstructor
public class RiegoController {

    private final RiegoControlService riegoControlService;

    @GetMapping("/estado")
    public RiegoEstadoResponse obtenerEstado() {
        return riegoControlService.obtenerEstado();
    }

    @GetMapping("/cultivos-activos")
    public List<CultivoResponse> listarCultivosActivos() {
        return riegoControlService.listarCultivosActivos();
    }

    @PostMapping("/modo")
    public RiegoEstadoResponse cambiarModo(@RequestBody RiegoModoRequest request) {
        return riegoControlService.cambiarModo(ModoOperacion.valueOf(request.modo().toUpperCase()));
    }

    @PostMapping("/perfil")
    public RiegoEstadoResponse seleccionarPerfilAutomatico(@RequestBody RiegoPerfilRequest request) {
        return riegoControlService.seleccionarPerfilAutomatico(request.cultivoId());
    }

    @PostMapping("/programacion")
    public RiegoEstadoResponse programarRiegoAutomatico(@RequestBody RiegoProgramacionRequest request) {
        return riegoControlService.programarRiegoAutomatico(request.cultivoId(), request.horaRiego());
    }

    @PostMapping("/manual")
    public RiegoEstadoResponse ejecutarOrdenManual(@RequestBody RiegoManualRequest request) {
        return riegoControlService.ejecutarOrdenManual(request.orden(), request.cultivoId());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> manejarForbidden(SecurityException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", mensajeError(exception)));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, NullPointerException.class})
    public ResponseEntity<Map<String, String>> manejarBadRequest(RuntimeException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", mensajeError(exception)));
    }

    private String mensajeError(Exception exception) {
        String mensaje = exception.getMessage();
        return mensaje != null && !mensaje.isBlank() ? mensaje : "Solicitud de riego invalida.";
    }
}
