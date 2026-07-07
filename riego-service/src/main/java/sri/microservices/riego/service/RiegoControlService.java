package sri.microservices.riego.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sri.microservices.riego.client.SensoresClient;
import sri.microservices.riego.dto.CultivoResponse;
import sri.microservices.riego.dto.RiegoEstadoResponse;
import sri.microservices.riego.dto.SensorData;
import sri.microservices.riego.model.ConfiguracionRiego;
import sri.microservices.riego.model.Cultivo;
import sri.microservices.riego.model.enums.ModoOperacion;
import sri.microservices.riego.model.enums.ModoRiego;
import sri.microservices.riego.repository.ConfiguracionRiegoRepository;
import sri.microservices.riego.repository.CultivoRepository;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class RiegoControlService {

    private static final int CONFIG_ID = 1;
    private static final int COMANDO_OFF = 0;
    private static final int COMANDO_ON = 1;

    private final ConfiguracionRiegoRepository configuracionRiegoRepository;
    private final CultivoRepository cultivoRepository;
    private final SensoresClient sensoresClient;
    private final EventoRiegoService eventoRiegoService;

    public RiegoControlService(
            ConfiguracionRiegoRepository configuracionRiegoRepository,
            CultivoRepository cultivoRepository,
            SensoresClient sensoresClient,
            EventoRiegoService eventoRiegoService
    ) {
        this.configuracionRiegoRepository = configuracionRiegoRepository;
        this.cultivoRepository = cultivoRepository;
        this.sensoresClient = sensoresClient;
        this.eventoRiegoService = eventoRiegoService;
    }

    public RiegoEstadoResponse obtenerEstado() {
        return toResponse(obtenerConfiguracion(), "Estado de riego consultado.");
    }

    public List<CultivoResponse> listarCultivosActivos() {
        return cultivoRepository.findByActivoTrue().stream().map(this::toCultivoResponse).toList();
    }

    @Transactional
    public RiegoEstadoResponse cambiarModo(ModoOperacion modoOperacion) {
        ConfiguracionRiego configuracion = obtenerConfiguracion();
        configuracion.setModoOperacion(modoOperacion);
        configuracionRiegoRepository.save(configuracion);
        return toResponse(configuracion, "Modo de operacion actualizado.");
    }

    @Transactional
    public RiegoEstadoResponse seleccionarPerfilAutomatico(Integer cultivoId) {
        ConfiguracionRiego configuracion = obtenerConfiguracion();
        configuracion.setCultivoActivo(obtenerCultivoActivo(cultivoId));
        configuracionRiegoRepository.save(configuracion);
        return toResponse(configuracion, "Perfil automatico seleccionado.");
    }

    @Transactional
    public RiegoEstadoResponse programarRiegoAutomatico(Integer cultivoId, String horaRiego) {
        if (horaRiego == null || horaRiego.isBlank()) {
            throw new IllegalArgumentException("La hora de riego es obligatoria.");
        }

        ConfiguracionRiego configuracion = obtenerConfiguracion();
        configuracion.setCultivoActivo(obtenerCultivoActivo(cultivoId));
        configuracion.setModoOperacion(ModoOperacion.AUTOMATICO);
        configuracion.setHoraRiegoProgramada(parseHoraRiego(horaRiego));
        configuracionRiegoRepository.save(configuracion);
        return toResponse(configuracion, "Programacion automatica guardada.");
    }

    @Transactional
    public RiegoEstadoResponse ejecutarOrdenManual(String orden, Integer cultivoId) {
        ConfiguracionRiego configuracion = obtenerConfiguracion();
        if (configuracion.getModoOperacion() == ModoOperacion.AUTOMATICO) {
            throw new SecurityException("El sistema esta en automatico. La orden manual fue rechazada.");
        }

        Cultivo cultivo = obtenerCultivoActivo(cultivoId);
        int comando = convertirOrdenAComando(orden);
        SensorData lecturaActual = sensoresClient.obtenerUltimaLectura();

        if (comando == COMANDO_ON) {
            if (lecturaActual == null || lecturaActual.humedad() == null) {
                throw new IllegalArgumentException("No hay una lectura de sensor disponible para iniciar el riego.");
            }
            configuracion.setCultivoActivo(cultivo);
            configuracionRiegoRepository.saveAndFlush(configuracion);
            sensoresClient.enviarComando(COMANDO_ON);
            eventoRiegoService.registrarInicio(cultivoId, ModoRiego.MANUAL, lecturaActual);
            return toResponse(configuracion, "Bomba encendida.");
        }

        sensoresClient.enviarComando(COMANDO_OFF);
        eventoRiegoService.completarRiego(lecturaActual);
        return toResponse(configuracion, "Bomba apagada.");
    }

    private ConfiguracionRiego obtenerConfiguracion() {
        return configuracionRiegoRepository.findById(CONFIG_ID)
                .orElseGet(this::crearConfiguracionInicial);
    }

    private ConfiguracionRiego crearConfiguracionInicial() {
        ConfiguracionRiego configuracion = new ConfiguracionRiego();
        configuracion.setId(CONFIG_ID);
        configuracion.setModoOperacion(ModoOperacion.MANUAL);
        return configuracionRiegoRepository.save(configuracion);
    }

    private Cultivo obtenerCultivoActivo(Integer cultivoId) {
        if (cultivoId == null) {
            throw new IllegalArgumentException("El cultivo es obligatorio.");
        }

        Cultivo cultivo = cultivoRepository.findById(cultivoId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de cultivo no encontrado."));

        if (!Boolean.TRUE.equals(cultivo.getActivo())) {
            throw new IllegalArgumentException("El perfil de cultivo esta inactivo y no puede usarse para riego.");
        }

        return cultivo;
    }

    private int convertirOrdenAComando(String orden) {
        if ("ON".equalsIgnoreCase(orden)) {
            return COMANDO_ON;
        }
        if ("OFF".equalsIgnoreCase(orden)) {
            return COMANDO_OFF;
        }
        throw new IllegalArgumentException("Orden de riego invalida.");
    }

    private LocalTime parseHoraRiego(String horaRiego) {
        try {
            return LocalTime.parse(horaRiego);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("La hora de riego debe tener formato HH:mm.");
        }
    }

    private RiegoEstadoResponse toResponse(ConfiguracionRiego configuracion, String mensaje) {
        ModoOperacion modo = configuracion.getModoOperacion();
        Cultivo cultivo = configuracion.getCultivoActivo();
        return new RiegoEstadoResponse(
                modo.name(),
                modo == ModoOperacion.AUTOMATICO,
                cultivo != null ? cultivo.getId() : null,
                cultivo != null ? cultivo.getNombre() : null,
                configuracion.getHoraRiegoProgramada() != null ? configuracion.getHoraRiegoProgramada().toString() : null,
                mensaje
        );
    }

    private CultivoResponse toCultivoResponse(Cultivo cultivo) {
        return new CultivoResponse(
                cultivo.getId(),
                cultivo.getNombre(),
                cultivo.getHumedadMinOptima(),
                cultivo.getHumedadMaxOptima(),
                cultivo.getDuracionRiegoMinutos(),
                cultivo.getTratoRecomendado(),
                cultivo.getActivo()
        );
    }
}
