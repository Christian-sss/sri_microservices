package sri.microservices.reportes.service;

import org.springframework.stereotype.Service;
import sri.microservices.reportes.dto.ConsumoAguaDetalleResponse;
import sri.microservices.reportes.dto.DistribucionModosResponse;
import sri.microservices.reportes.dto.EstadisticasResumenResponse;
import sri.microservices.reportes.dto.TelemetriaResponse;
import sri.microservices.reportes.model.ConfiguracionRiego;
import sri.microservices.reportes.model.Cultivo;
import sri.microservices.reportes.model.EventoRiego;
import sri.microservices.reportes.model.LecturaSensor;
import sri.microservices.reportes.model.enums.EstadoRiego;
import sri.microservices.reportes.model.enums.ModoRiego;
import sri.microservices.reportes.repository.ConfiguracionRiegoRepository;
import sri.microservices.reportes.repository.CultivoRepository;
import sri.microservices.reportes.repository.EventoRiegoRepository;
import sri.microservices.reportes.repository.LecturaSensorRepository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class EstadisticasService {

    private static final ZoneId ZONA_APP = ZoneId.of("America/Lima");

    private final EventoRiegoRepository eventoRiegoRepository;
    private final LecturaSensorRepository lecturaSensorRepository;
    private final ConfiguracionRiegoRepository configuracionRiegoRepository;
    private final CultivoRepository cultivoRepository;

    public EstadisticasService(EventoRiegoRepository eventoRiegoRepository,
                               LecturaSensorRepository lecturaSensorRepository,
                               ConfiguracionRiegoRepository configuracionRiegoRepository,
                               CultivoRepository cultivoRepository) {
        this.eventoRiegoRepository = eventoRiegoRepository;
        this.lecturaSensorRepository = lecturaSensorRepository;
        this.configuracionRiegoRepository = configuracionRiegoRepository;
        this.cultivoRepository = cultivoRepository;
    }

    public EstadisticasResumenResponse obtenerResumen() {
        Map<String, Integer> modos = obtenerDatosModosRiego();
        long manuales = modos.getOrDefault("MANUAL", 0);
        long automaticos = modos.getOrDefault("AUTOMATICO", 0);
        DatosDuracion duracion = obtenerDuracionUltimos7Dias();

        return new EstadisticasResumenResponse(
                manuales + automaticos,
                manuales,
                automaticos,
                eventoRiegoRepository.countByEstado(EstadoRiego.COMPLETADO),
                redondear(eventoRiegoRepository.obtenerPromedioHumedadGanada()),
                obtenerUltimoRiegoTexto(),
                duracion.labels(),
                duracion.valores()
        );
    }

    public List<TelemetriaResponse> obtenerTelemetriaReciente(Integer cultivoId, boolean soloMantenimiento) {
        List<LecturaSensor> lecturas;
        if (soloMantenimiento) {
            lecturas = lecturaSensorRepository.findTop20ByCultivoIdIsNullOrderByFechaLecturaDesc();
        } else if (cultivoId != null) {
            lecturas = lecturaSensorRepository.findTop20ByCultivoIdOrderByFechaLecturaDesc(cultivoId);
        } else {
            lecturas = lecturaSensorRepository.findTop20ByOrderByFechaLecturaDesc();
        }

        return lecturas.stream()
                .sorted(Comparator.comparing(LecturaSensor::getFechaLectura, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toTelemetriaResponse)
                .toList();
    }

    public DistribucionModosResponse obtenerDistribucionModosMesActual(Integer cultivoId, boolean soloMantenimiento) {
        LocalDate hoy = LocalDate.now(ZONA_APP);
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = inicioMes.plusMonths(1);

        long manual = contarRiegosPorFiltro(ModoRiego.MANUAL, cultivoId, soloMantenimiento, inicioMes, finMes);
        long automatico = contarRiegosPorFiltro(ModoRiego.AUTOMATICO, cultivoId, soloMantenimiento, inicioMes, finMes);

        return new DistribucionModosResponse(manual, automatico, manual + automatico, obtenerEtiquetaCultivo(cultivoId, soloMantenimiento));
    }

    public List<ConsumoAguaDetalleResponse> obtenerConsumoAguaDetalle(Integer cultivoId, boolean soloMantenimiento) {
        return eventoRiegoRepository.obtenerConsumoAguaDetalle(cultivoId, soloMantenimiento ? 1 : 0)
                .stream()
                .map(fila -> new ConsumoAguaDetalleResponse(
                        String.valueOf(fila[0]),
                        String.valueOf(fila[1]),
                        String.valueOf(fila[2]),
                        String.valueOf(fila[3]),
                        fila[4] instanceof Number numero ? numero.doubleValue() : 0.0
                ))
                .toList();
    }

    private Map<String, Integer> obtenerDatosModosRiego() {
        Map<String, Integer> datos = new LinkedHashMap<>();
        datos.put("MANUAL", 0);
        datos.put("AUTOMATICO", 0);
        LocalDate hoy = LocalDate.now(ZONA_APP);
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = inicioMes.plusMonths(1);
        datos.put("MANUAL", Math.toIntExact(eventoRiegoRepository.countByModoRiegoAndFechaInicioBetween(
                ModoRiego.MANUAL, inicioMes, finMes)));
        datos.put("AUTOMATICO", Math.toIntExact(eventoRiegoRepository.countByModoRiegoAndFechaInicioBetween(
                ModoRiego.AUTOMATICO, inicioMes, finMes)));
        return datos;
    }

    private long contarRiegosPorFiltro(ModoRiego modoRiego, Integer cultivoId, boolean soloMantenimiento, LocalDateTime inicio, LocalDateTime fin) {
        if (soloMantenimiento) {
            return eventoRiegoRepository.countByModoRiegoAndCultivoIsNullAndFechaInicioBetween(modoRiego, inicio, fin);
        }
        if (cultivoId != null) {
            return eventoRiegoRepository.countByModoRiegoAndCultivo_IdAndFechaInicioBetween(modoRiego, cultivoId, inicio, fin);
        }
        return eventoRiegoRepository.countByModoRiegoAndFechaInicioBetween(modoRiego, inicio, fin);
    }

    private TelemetriaResponse toTelemetriaResponse(LecturaSensor lectura) {
        LocalDateTime fecha = lectura.getFechaLectura();
        return new TelemetriaResponse(
                fecha != null ? fecha.toString() : null,
                fecha != null ? fecha.format(DateTimeFormatter.ofPattern("HH:mm")) : "--:--",
                lectura.getHumedadSuelo(),
                lectura.getDistanciaAgua()
        );
    }

    private String obtenerEtiquetaCultivo(Integer cultivoId, boolean soloMantenimiento) {
        if (soloMantenimiento) {
            return "Pruebas / Mantenimiento";
        }
        if (cultivoId != null) {
            return cultivoRepository.findById(cultivoId).map(Cultivo::getNombre).orElse("Cultivo " + cultivoId);
        }
        return configuracionRiegoRepository.findById(1)
                .map(ConfiguracionRiego::getCultivoActivo)
                .map(Cultivo::getNombre)
                .orElse("Sin cultivo");
    }

    private String obtenerUltimoRiegoTexto() {
        EventoRiego ultimo = eventoRiegoRepository.findTopByOrderByFechaInicioDesc();
        return ultimo == null || ultimo.getFechaInicio() == null
                ? "Sin registros"
                : ultimo.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
    }

    private DatosDuracion obtenerDuracionUltimos7Dias() {
        List<String> labels = new ArrayList<>();
        List<Long> valores = new ArrayList<>();
        LocalDateTime inicio = LocalDate.now(ZONA_APP).minusDays(6).atStartOfDay();
        for (Object[] fila : eventoRiegoRepository.obtenerDuracionDiariaUltimos7Dias(inicio)) {
            labels.add(formatearFecha(fila[0]));
            valores.add(fila[1] instanceof Number numero ? numero.longValue() : 0L);
        }
        return new DatosDuracion(labels, valores);
    }

    private String formatearFecha(Object valor) {
        return valor instanceof Date fecha
                ? fecha.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM"))
                : String.valueOf(valor);
    }

    private double redondear(Double valor) {
        return valor == null ? 0 : Math.round(valor * 10.0) / 10.0;
    }

    private record DatosDuracion(List<String> labels, List<Long> valores) {
    }
}
