# sensores-service

Microservicio de sensores y MQTT.

## Rutas

- `GET /api/mqtt`
- `POST /api/mqtt/connect`
- `POST /api/mqtt/disconnect`
- `GET /api/mqtt/status`
- `GET /api/sensor/ultima-lectura`
- `POST /api/sensor/comando`
- `GET /api/estado-vivo`

## Variables de entorno

- `PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `MQTT_BROKER`
- `MQTT_CLIENT_ID`
