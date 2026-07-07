package sri.microservices.sensores.service;

import org.springframework.stereotype.Service;
import sri.microservices.sensores.dto.SensorData;
import sri.microservices.sensores.model.LecturaSensor;
import sri.microservices.sensores.repository.LecturaSensorRepository;

@Service
public class ProcesarDatosService {

    private final LecturaSensorRepository lecturaSensorRepository;

    public ProcesarDatosService(LecturaSensorRepository lecturaSensorRepository) {
        this.lecturaSensorRepository = lecturaSensorRepository;
    }

    public void procesar(SensorData data) {
        LecturaSensor lectura = new LecturaSensor();
        lectura.setHumedadSuelo(data.humedad());
        lectura.setDistanciaAgua(data.distancia());
        lecturaSensorRepository.save(lectura);
    }
}
