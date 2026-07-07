package sri.microservices.riego.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sri.microservices.riego.client.SensoresClient;
import sri.microservices.riego.dto.SensorData;
import sri.microservices.riego.model.ConfiguracionRiego;
import sri.microservices.riego.model.Cultivo;
import sri.microservices.riego.model.EventoRiego;
import sri.microservices.riego.model.enums.ModoOperacion;
import sri.microservices.riego.model.enums.ModoRiego;
import sri.microservices.riego.repository.ConfiguracionRiegoRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Component
public class RiegoAutomaticoScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RiegoAutomaticoScheduler.class);
    private static final int CONFIG_ID = 1;
    private static final int COMANDO_OFF = 0;
    private static final int COMANDO_ON = 1;
    private static final ZoneId ZONA_RAILWAY_APP = ZoneId.of("America/Lima");

    private final ConfiguracionRiegoRepository configuracionRiegoRepository;
    private final SensoresClient sensoresClient;
    private final EventoRiegoService eventoRiegoService;

    public RiegoAutomaticoScheduler(
            ConfiguracionRiegoRepository configuracionRiegoRepository,
            SensoresClient sensoresClient,
            EventoRiegoService eventoRiegoService
    ) {
        this.configuracionRiegoRepository = configuracionRiegoRepository;
        this.sensoresClient = sensoresClient;
        this.eventoRiegoService = eventoRiegoService;
    }

    @Scheduled(cron = "0 * * * * *", zone = "America/Lima")
    public void ejecutarRiegoProgramado() {
        try {
            ConfiguracionRiego configuracion = configuracionRiegoRepository.findById(CONFIG_ID).orElse(null);
            if (!configuracionListaParaEjecutar(configuracion)) {
                return;
            }

            LocalDate hoy = LocalDate.now(ZONA_RAILWAY_APP);
            LocalTime ahora = LocalTime.now(ZONA_RAILWAY_APP).truncatedTo(ChronoUnit.MINUTES);
            LocalTime horaProgramada = configuracion.getHoraRiegoProgramada().truncatedTo(ChronoUnit.MINUTES);
            if (!horaProgramada.equals(ahora)) {
                return;
            }

            LocalDateTime inicioDia = hoy.atStartOfDay();
            LocalDateTime finDia = hoy.plusDays(1).atStartOfDay();
            if (eventoRiegoService.existeRiegoAutomaticoEntre(inicioDia, finDia)) {
                return;
            }

            SensorData lecturaActual = sensoresClient.obtenerUltimaLectura();
            boolean iniciado = eventoRiegoService.registrarInicio(
                    configuracion.getCultivoActivo().getId(),
                    ModoRiego.AUTOMATICO,
                    lecturaActual
            );

            if (iniciado) {
                sensoresClient.enviarComando(COMANDO_ON);
                LOGGER.info("Riego automatico iniciado para cultivo {}.", configuracion.getCultivoActivo().getId());
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("No se pudo ejecutar el riego automatico programado: {}", exception.getMessage());
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void detenerRiegoAutomaticoCompletado() {
        try {
            EventoRiego evento = eventoRiegoService.obtenerRiegoEnProceso().orElse(null);
            if (evento == null || evento.getModoRiego() != ModoRiego.AUTOMATICO || evento.getCultivo() == null) {
                return;
            }

            int duracionMinutos = obtenerDuracionMinutos(evento.getCultivo());
            long minutosTranscurridos = Duration.between(evento.getFechaInicio(), LocalDateTime.now()).toMinutes();
            if (minutosTranscurridos < duracionMinutos) {
                return;
            }

            SensorData lecturaFinal = sensoresClient.obtenerUltimaLectura();
            sensoresClient.enviarComando(COMANDO_OFF);
            eventoRiegoService.completarRiego(lecturaFinal);
            LOGGER.info("Riego automatico completado para cultivo {}.", evento.getCultivo().getId());
        } catch (RuntimeException exception) {
            LOGGER.warn("No se pudo detener el riego automatico: {}", exception.getMessage());
        }
    }

    private boolean configuracionListaParaEjecutar(ConfiguracionRiego configuracion) {
        return configuracion != null
                && configuracion.getModoOperacion() == ModoOperacion.AUTOMATICO
                && configuracion.getCultivoActivo() != null
                && configuracion.getHoraRiegoProgramada() != null;
    }

    private int obtenerDuracionMinutos(Cultivo cultivo) {
        Integer duracion = cultivo.getDuracionRiegoMinutos();
        return duracion != null && duracion > 0 ? duracion : 1;
    }
}
