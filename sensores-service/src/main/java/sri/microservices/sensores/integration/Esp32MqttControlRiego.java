package sri.microservices.sensores.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Esp32MqttControlRiego {

    private static final int COMANDO_LOGICO_OFF = 0;
    private static final int COMANDO_LOGICO_ON = 1;

    private final Esp32MqttConnectionManager mqtt;
    private final int payloadOn;
    private final int payloadOff;
    private volatile boolean bombaActiva;

    public Esp32MqttControlRiego(
            Esp32MqttConnectionManager mqtt,
            @Value("${riego.mqtt.payload-on:${RIEGO_MQTT_PAYLOAD_ON:1}}") int payloadOn,
            @Value("${riego.mqtt.payload-off:${RIEGO_MQTT_PAYLOAD_OFF:0}}") int payloadOff
    ) {
        this.mqtt = mqtt;
        this.payloadOn = payloadOn;
        this.payloadOff = payloadOff;
    }

    public void enviarComando(int comando) {
        if (!mqtt.estaConectado()) {
            throw new IllegalStateException("MQTT no esta conectado.");
        }

        int payload = convertirComandoAPayload(comando);
        mqtt.publish("upt/riego/orden", String.valueOf(payload));
        bombaActiva = comando == COMANDO_LOGICO_ON;
    }

    public boolean isBombaActiva() {
        return bombaActiva;
    }

    public void confirmarBombaApagada() {
        bombaActiva = false;
    }

    private int convertirComandoAPayload(int comando) {
        if (comando == COMANDO_LOGICO_ON) {
            return payloadOn;
        }
        if (comando == COMANDO_LOGICO_OFF) {
            return payloadOff;
        }
        throw new IllegalArgumentException("Comando de riego invalido.");
    }
}
