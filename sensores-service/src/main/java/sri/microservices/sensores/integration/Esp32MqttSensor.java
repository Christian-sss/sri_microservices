package sri.microservices.sensores.integration;

import lombok.Getter;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;
import sri.microservices.sensores.dto.SensorData;
import sri.microservices.sensores.service.AlertaRiegoService;
import sri.microservices.sensores.service.ProcesarDatosService;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class Esp32MqttSensor {

    private static final ZoneId ZONA_APP = ZoneId.of("America/Lima");

    private final Esp32MqttConnectionManager mqtt;
    private final ProcesarDatosService procesarDatosService;
    private final Esp32MqttControlRiego mqttControlRiego;
    private final AlertaRiegoService alertaRiegoService;

    @Getter
    private volatile SensorData ultimoDato;

    @Getter
    private volatile LocalDateTime ultimaLecturaEn;

    private MqttClient clienteSuscrito;

    public Esp32MqttSensor(
            Esp32MqttConnectionManager mqtt,
            ProcesarDatosService procesarDatosService,
            Esp32MqttControlRiego mqttControlRiego,
            AlertaRiegoService alertaRiegoService
    ) {
        this.mqtt = mqtt;
        this.procesarDatosService = procesarDatosService;
        this.mqttControlRiego = mqttControlRiego;
        this.alertaRiegoService = alertaRiegoService;
    }

    public synchronized void iniciar() {
        try {
            if (!mqtt.estaConectado() || mqtt.getClient() == null) {
                throw new IllegalStateException("MQTT debe estar conectado antes de suscribir los sensores.");
            }

            MqttClient client = mqtt.getClient();
            if (client == clienteSuscrito) {
                return;
            }

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    clienteSuscrito = null;
                    System.err.println("[MQTT] Conexion perdida");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    parsearLinea(new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            client.subscribe("upt/riego/datos");
            clienteSuscrito = client;
        } catch (MqttException e) {
            throw new IllegalStateException("[MQTT] Error al suscribir los sensores", e);
        }
    }

    private void parsearLinea(String linea) {
        try {
            String payload = linea == null ? "" : linea.trim();
            String[] partes = payload.split(",", -1);

            if (partes.length < 2 || partes[0].isBlank() || partes[1].isBlank()) {
                System.err.println("[MQTT] Payload de sensor invalido: " + payload);
                return;
            }

            int humedad = Integer.parseInt(partes[0].trim());
            double distancia = Double.parseDouble(partes[1].trim());
            Boolean bombaActiva = partes.length >= 3 && !partes[2].isBlank()
                    ? "1".equals(partes[2].trim())
                    : null;

            SensorData sensorData = new SensorData(humedad, distancia, bombaActiva);
            ultimoDato = sensorData;
            ultimaLecturaEn = LocalDateTime.now(ZONA_APP);
            procesarDatosService.procesar(sensorData);
            detectarTanqueVacio(sensorData);
        } catch (Exception e) {
            System.err.println("[MQTT] Error parseando payload de sensor");
            e.printStackTrace();
        }
    }

    private void detectarTanqueVacio(SensorData sensorData) {
        if (sensorData.distancia() < 18 || !mqttControlRiego.isBombaActiva()) {
            return;
        }

        mqttControlRiego.confirmarBombaApagada();
        alertaRiegoService.registrarTanqueVacio();
    }
}
