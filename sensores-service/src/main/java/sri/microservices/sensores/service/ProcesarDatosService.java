package sri.microservices.sensores.service;

import org.springframework.stereotype.Service;
import sri.microservices.sensores.dto.SensorData;
import sri.microservices.sensores.model.LecturaSensor;
import sri.microservices.sensores.repository.LecturaSensorRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class ProcesarDatosService {

    private static final ZoneId ZONA_APP = ZoneId.of("America/Lima");

    private final LecturaSensorRepository lecturaSensorRepository;

    public ProcesarDatosService(LecturaSensorRepository lecturaSensorRepository) {
        this.lecturaSensorRepository = lecturaSensorRepository;
    }

    public void procesar(SensorData data) {
        LecturaSensor lectura = new LecturaSensor();
        lectura.setHumedadSuelo(data.humedad());
        lectura.setDistanciaAgua(data.distancia());
        lectura.setFechaLectura(LocalDateTime.now(ZONA_APP));
        lecturaSensorRepository.save(lectura);
    }
}
