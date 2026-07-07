package sri.microservices.sensores.service;

import org.springframework.stereotype.Service;
import sri.microservices.sensores.dto.AlertaRiegoResponse;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AlertaRiegoService {

    private static final ZoneId ZONA_APP = ZoneId.of("America/Lima");

    private final AtomicLong secuencia = new AtomicLong();
    private volatile AlertaRiegoResponse ultimaAlerta;

    public void registrarTanqueVacio() {
        ultimaAlerta = crear(
                "danger",
                "Bomba detenida por seguridad",
                "El tanque se quedo sin agua. Revise el nivel antes de reiniciar el riego."
        );
    }

    public synchronized AlertaRiegoResponse consumirUltimaAlerta() {
        AlertaRiegoResponse alerta = ultimaAlerta;
        ultimaAlerta = null;
        return alerta;
    }

    private AlertaRiegoResponse crear(String tipo, String titulo, String mensaje) {
        return new AlertaRiegoResponse(
                secuencia.incrementAndGet(),
                tipo,
                titulo,
                mensaje,
                LocalDateTime.now(ZONA_APP).toString()
        );
    }
}
