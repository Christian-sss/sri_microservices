package sri.microservices.riego.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sri.microservices.riego.dto.SensorData;
import sri.microservices.riego.model.Cultivo;
import sri.microservices.riego.model.EventoRiego;
import sri.microservices.riego.model.enums.EstadoRiego;
import sri.microservices.riego.model.enums.ModoRiego;
import sri.microservices.riego.repository.CultivoRepository;
import sri.microservices.riego.repository.EventoRiegoRepository;

import java.time.LocalDateTime;

@Service
public class EventoRiegoService {

    private final EventoRiegoRepository eventoRiegoRepository;
    private final CultivoRepository cultivoRepository;

    public EventoRiegoService(EventoRiegoRepository eventoRiegoRepository, CultivoRepository cultivoRepository) {
        this.eventoRiegoRepository = eventoRiegoRepository;
        this.cultivoRepository = cultivoRepository;
    }

    @Transactional
    public void registrarInicio(Integer cultivoId, ModoRiego modoRiego, SensorData lecturaInicial) {
        if (lecturaInicial == null || lecturaInicial.humedad() == null) {
            throw new IllegalArgumentException("No hay una lectura de sensor disponible para iniciar el riego.");
        }

        if (eventoRiegoRepository.findFirstByEstadoOrderByFechaInicioDesc(EstadoRiego.EN_PROCESO).isPresent()) {
            return;
        }

        Cultivo cultivo = cultivoRepository.findById(cultivoId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de cultivo no encontrado."));

        EventoRiego evento = new EventoRiego();
        evento.setCultivo(cultivo);
        evento.setModoRiego(modoRiego);
        evento.setFechaInicio(LocalDateTime.now());
        evento.setHumedadSueloInicial(lecturaInicial.humedad());
        evento.setEstado(EstadoRiego.EN_PROCESO);
        eventoRiegoRepository.save(evento);
    }

    @Transactional
    public void completarRiego(SensorData lecturaFinal) {
        eventoRiegoRepository.findFirstByEstadoOrderByFechaInicioDesc(EstadoRiego.EN_PROCESO)
                .ifPresent(evento -> {
                    evento.setFechaFin(LocalDateTime.now());
                    evento.setHumedadSueloFinal(lecturaFinal != null && lecturaFinal.humedad() != null
                            ? lecturaFinal.humedad()
                            : evento.getHumedadSueloInicial());
                    evento.setEstado(EstadoRiego.COMPLETADO);
                    eventoRiegoRepository.save(evento);
                });
    }
}
