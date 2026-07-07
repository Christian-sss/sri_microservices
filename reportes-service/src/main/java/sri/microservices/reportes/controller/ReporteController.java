package sri.microservices.reportes.controller;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sri.microservices.reportes.service.ReporteService;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/descargar-pdf")
    public ResponseEntity<byte[]> descargarReporteRiegos(@RequestParam(required = false) LocalDate fechaInicio,
                                                         @RequestParam(required = false) LocalDate fechaFin,
                                                         @RequestParam(required = false) String cultivoId) {
        return descargarPdf(() -> reporteService.generarReporteModosRiegoPDF(fechaInicio, fechaFin, cultivoId),
                "Reporte_Sistema_Riego.pdf");
    }

    @GetMapping("/consumo-agua")
    public ResponseEntity<byte[]> descargarReporteConsumoAgua(@RequestParam(required = false) LocalDate fechaInicio,
                                                              @RequestParam(required = false) LocalDate fechaFin,
                                                              @RequestParam(required = false) String cultivoId) {
        return descargarPdf(() -> reporteService.generarReporteConsumoAguaPDF(fechaInicio, fechaFin, cultivoId),
                "Reporte_Consumo_Agua.pdf");
    }

    private ResponseEntity<byte[]> descargarPdf(GeneradorPdf generador, String filename) {
        try {
            byte[] reportePdf = generador.generar();
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(reportePdf.length);
            return new ResponseEntity<>(reportePdf, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"" + e.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8));
        } catch (JRException | FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"No se pudo generar el PDF.\"}").getBytes(StandardCharsets.UTF_8));
        }
    }

    @FunctionalInterface
    private interface GeneradorPdf {
        byte[] generar() throws JRException, FileNotFoundException;
    }
}
