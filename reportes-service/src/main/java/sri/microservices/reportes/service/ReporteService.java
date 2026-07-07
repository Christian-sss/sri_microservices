package sri.microservices.reportes.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReporteService {

    private static final String CULTIVO_MANTENIMIENTO = "mantenimiento";
    private static final String NOMBRE_MANTENIMIENTO = "Mantenimiento / Sin Cultivo";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ReporteService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public byte[] generarReporteModosRiegoPDF(LocalDate fechaInicio, LocalDate fechaFin, String cultivoId)
            throws JRException, FileNotFoundException {
        FiltroReporte filtro = resolverFiltro(cultivoId);
        LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = fechaFin != null ? fechaFin.plusDays(1).atStartOfDay() : null;

        Collection<Map<String, ?>> datosReporte = new ArrayList<>();
        for (Map<String, Object> fila : obtenerFilasReporte(inicio, fin, filtro)) {
            datosReporte.add(toReporteRow(fila));
        }

        Collection<Map<String, ?>> resumenModos = new ArrayList<>();
        for (Map<String, Object> fila : obtenerResumenModos(inicio, fin, filtro)) {
            Map<String, Object> row = new HashMap<>();
            row.put("modoRiego", texto(valor(fila, "modoRiego", "modo_riego"), "-"));
            row.put("cantidad", toLong(valor(fila, "cantidad")));
            resumenModos.add(row);
        }

        Collection<Map<String, ?>> sensorDataList = new ArrayList<>();
        for (Map<String, Object> fila : obtenerLecturasSensor(filtro)) {
            Map<String, Object> row = new HashMap<>();
            row.put("fecha", texto(valor(fila, "fecha"), "--:--"));
            row.put("humedad", numero(valor(fila, "humedad")));
            sensorDataList.add(row);
        }

        JasperReport jasperReport = cargarReporteDesdeJrxml("reportes/grafico_modos_riego.jrxml");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("creadoPor", "Sistema SRI - Administrador");
        parametros.put("fechaGeneracion", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        parametros.put("TITULO_FILTRO", obtenerTituloFiltro(filtro));
        parametros.put("sensorDataSource", new JRMapCollectionDataSource(sensorDataList));
        parametros.put("modosDataSource", new JRMapCollectionDataSource(resumenModos));

        JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport,
                parametros,
                new JRMapCollectionDataSource(datosReporte)
        );
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    public byte[] generarReporteConsumoAguaPDF(LocalDate fechaInicio, LocalDate fechaFin, String cultivoId)
            throws JRException, FileNotFoundException {
        FiltroReporte filtro = resolverFiltro(cultivoId);
        LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = fechaFin != null ? fechaFin.plusDays(1).atStartOfDay() : null;

        Collection<Map<String, ?>> datosReporte = new ArrayList<>();
        double totalLitros = 0.0;
        for (Map<String, Object> fila : obtenerFilasConsumoAgua(inicio, fin, filtro)) {
            Map<String, Object> row = toConsumoAguaRow(fila);
            datosReporte.add(row);
            totalLitros += ((Number) row.get("litrosConsumidos")).doubleValue();
        }

        JasperReport jasperReport = cargarReporteDesdeJrxml("reportes/consumo_agua.jrxml");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("creadoPor", "Sistema SRI - Administrador");
        parametros.put("fechaGeneracion", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        parametros.put("TITULO_FILTRO", obtenerTituloFiltro(filtro));
        parametros.put("TOTAL_LITROS", Math.round(totalLitros * 100.0) / 100.0);

        JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport,
                parametros,
                new JRMapCollectionDataSource(datosReporte)
        );
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    private JasperReport cargarReporteDesdeJrxml(String rutaClasspath) throws JRException, FileNotFoundException {
        ClassPathResource recurso = new ClassPathResource(rutaClasspath);
        try (InputStream reporteStream = recurso.getInputStream()) {
            return JasperCompileManager.compileReport(reporteStream);
        } catch (IOException e) {
            FileNotFoundException exception = new FileNotFoundException("No se pudo cargar la plantilla: " + rutaClasspath);
            exception.initCause(e);
            throw exception;
        }
    }

    private List<Map<String, Object>> obtenerFilasReporte(LocalDateTime inicio, LocalDateTime fin, FiltroReporte filtro) {
        StringBuilder sql = new StringBuilder("""
                SELECT e.id AS id, e.fecha_inicio AS fechaInicio,
                       COALESCE(c.nombre, 'Mantenimiento / Sin Cultivo') AS nombreCultivo,
                       e.modo_riego AS modoRiego, e.humedad_suelo_inicial AS humedadSueloInicial,
                       e.humedad_suelo_final AS humedadSueloFinal, e.estado AS estado,
                       'Optimo' AS estadoUltrasonico, 1 AS cantidad
                FROM eventos_riego e
                LEFT JOIN perfiles_cultivo c ON c.id = e.cultivo_id
                WHERE 1 = 1
                """);
        MapSqlParameterSource params = aplicarFiltros(sql, inicio, fin, filtro);
        sql.append(" ORDER BY e.fecha_inicio DESC");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private List<Map<String, Object>> obtenerResumenModos(LocalDateTime inicio, LocalDateTime fin, FiltroReporte filtro) {
        StringBuilder sql = new StringBuilder("SELECT e.modo_riego AS modoRiego, COUNT(e.id) AS cantidad FROM eventos_riego e WHERE 1 = 1");
        MapSqlParameterSource params = aplicarFiltros(sql, inicio, fin, filtro);
        sql.append(" GROUP BY e.modo_riego ORDER BY e.modo_riego");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private List<Map<String, Object>> obtenerFilasConsumoAgua(LocalDateTime inicio, LocalDateTime fin, FiltroReporte filtro) {
        StringBuilder sql = new StringBuilder("""
                SELECT DATE_FORMAT(e.fecha_inicio, '%Y-%m-%d') AS fecha,
                       COALESCE(c.nombre, 'Mantenimiento / Sin Cultivo') AS nombreCultivo,
                       ROUND(SUM(TIMESTAMPDIFF(SECOND, e.fecha_inicio, e.fecha_fin)) / 60.0, 2) AS duracionMinutos,
                       ROUND((SUM(TIMESTAMPDIFF(SECOND, e.fecha_inicio, e.fecha_fin)) / 60.0) * 1.0, 2) AS litrosConsumidos
                FROM eventos_riego e
                LEFT JOIN perfiles_cultivo c ON c.id = e.cultivo_id
                WHERE e.estado = 'COMPLETADO' AND e.fecha_fin IS NOT NULL
                """);
        MapSqlParameterSource params = aplicarFiltros(sql, inicio, fin, filtro);
        sql.append(" GROUP BY DATE(e.fecha_inicio), e.cultivo_id, c.nombre ORDER BY DATE(e.fecha_inicio) DESC, c.nombre");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private List<Map<String, Object>> obtenerLecturasSensor(FiltroReporte filtro) {
        String sql = """
                SELECT DATE_FORMAT(fecha_lectura, '%H:%i') AS fecha, humedad_suelo AS humedad
                FROM lecturas_sensor
                WHERE (:cultivoId IS NULL OR cultivo_id = :cultivoId)
                ORDER BY fecha_lectura DESC
                LIMIT 20
                """;
        return jdbcTemplate.queryForList(sql, new MapSqlParameterSource("cultivoId", filtro.cultivoId()));
    }

    private MapSqlParameterSource aplicarFiltros(StringBuilder sql, LocalDateTime inicio, LocalDateTime fin, FiltroReporte filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (inicio != null) {
            sql.append(" AND e.fecha_inicio >= :fechaInicio");
            params.addValue("fechaInicio", inicio);
        }
        if (fin != null) {
            sql.append(" AND e.fecha_inicio < :fechaFin");
            params.addValue("fechaFin", fin);
        }
        if (filtro.soloMantenimiento()) {
            sql.append(" AND e.cultivo_id IS NULL");
        } else if (filtro.cultivoId() != null) {
            sql.append(" AND e.cultivo_id = :cultivoId");
            params.addValue("cultivoId", filtro.cultivoId());
        }
        return params;
    }

    private Map<String, Object> toReporteRow(Map<String, Object> fila) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", valor(fila, "id"));
        row.put("fechaInicio", formatearFecha(valor(fila, "fechaInicio", "fecha_inicio")));
        row.put("nombreCultivo", texto(valor(fila, "nombreCultivo", "nombre_cultivo"), NOMBRE_MANTENIMIENTO));
        row.put("modoRiego", texto(valor(fila, "modoRiego", "modo_riego"), "-"));
        row.put("humedadSueloInicial", valor(fila, "humedadSueloInicial", "humedad_suelo_inicial"));
        row.put("humedadSueloFinal", valor(fila, "humedadSueloFinal", "humedad_suelo_final"));
        row.put("estado", texto(valor(fila, "estado"), "-"));
        row.put("estadoUltrasonico", texto(valor(fila, "estadoUltrasonico", "estado_ultrasonico"), "Optimo"));
        row.put("cantidad", valor(fila, "cantidad") != null ? valor(fila, "cantidad") : 1);
        return row;
    }

    private Map<String, Object> toConsumoAguaRow(Map<String, Object> fila) {
        Map<String, Object> row = new HashMap<>();
        row.put("fecha", texto(valor(fila, "fecha"), "-"));
        row.put("nombreCultivo", texto(valor(fila, "nombreCultivo", "nombre_cultivo"), NOMBRE_MANTENIMIENTO));
        row.put("duracionMinutos", numero(valor(fila, "duracionMinutos", "duracion_minutos")));
        row.put("litrosConsumidos", numero(valor(fila, "litrosConsumidos", "litros_consumidos")));
        return row;
    }

    private FiltroReporte resolverFiltro(String cultivoId) {
        if (cultivoId == null || cultivoId.isBlank()) {
            return new FiltroReporte(null, false);
        }
        if (CULTIVO_MANTENIMIENTO.equalsIgnoreCase(cultivoId.trim())) {
            return new FiltroReporte(null, true);
        }
        return new FiltroReporte(Integer.valueOf(cultivoId), false);
    }

    private String obtenerTituloFiltro(FiltroReporte filtro) {
        if (filtro.soloMantenimiento()) {
            return NOMBRE_MANTENIMIENTO;
        }
        if (filtro.cultivoId() != null) {
            return "Cultivo " + filtro.cultivoId();
        }
        return "Reporte General";
    }

    private Object valor(Map<String, Object> fila, String... claves) {
        for (String clave : claves) {
            if (fila.containsKey(clave)) {
                return fila.get(clave);
            }
        }
        return null;
    }

    private String texto(Object valor, String valorPorDefecto) {
        return valor != null ? String.valueOf(valor) : valorPorDefecto;
    }

    private Double numero(Object valor) {
        return valor instanceof Number numero ? numero.doubleValue() : 0.0;
    }

    private Long toLong(Object valor) {
        return valor instanceof Number numero ? numero.longValue() : 0L;
    }

    private String formatearFecha(Object valor) {
        if (valor instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
        if (valor instanceof LocalDateTime fecha) {
            return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
        return valor != null ? String.valueOf(valor) : "-";
    }

    private record FiltroReporte(Integer cultivoId, boolean soloMantenimiento) {
    }
}
