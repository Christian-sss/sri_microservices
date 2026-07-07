package sri.microservices.cultivos.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.microservices.cultivos.dto.CultivoRequest;
import sri.microservices.cultivos.dto.CultivoResponse;
import sri.microservices.cultivos.service.CultivoService;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/cultivos")
public class CultivoController {

    private final CultivoService cultivoService;

    @GetMapping
    public List<CultivoResponse> listar() {
        return cultivoService.listarTodos();
    }

    @GetMapping("/activos")
    public List<CultivoResponse> listarActivos() {
        return cultivoService.listarActivos();
    }

    @GetMapping("/inactivos")
    public List<CultivoResponse> listarInactivos() {
        return cultivoService.listarInactivos();
    }

    @GetMapping("/{id}")
    public CultivoResponse obtenerPorId(@PathVariable Integer id) {
        return cultivoService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<CultivoResponse> crear(@RequestBody CultivoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cultivoService.crear(request));
    }

    @PutMapping("/{id}")
    public CultivoResponse actualizar(@PathVariable Integer id, @RequestBody CultivoRequest request) {
        return cultivoService.actualizar(id, request);
    }

    @PutMapping("/{id}/toggle-estado")
    public ResponseEntity<Void> toggleEstado(@PathVariable Integer id) {
        cultivoService.toggleEstado(id);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarErrores(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
