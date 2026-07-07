# SRI Microservicios

Estructura inicial para separar el backend SRI en servicios Spring Boot independientes.

## Servicios

- `api-gateway`: entrada unica para el frontend.
- `auth-service`: login, registro, logout y usuario actual.
- `cultivos-service`: CRUD de cultivos, activos e inactivos.
- `riego-service`: riego manual, automatico y programaciones.
- `sensores-service`: MQTT, ESP32 y lecturas de sensores.
- `reportes-service`: reportes, PDF y estadisticas.
- `chat-service`: chatbot/asistente del sistema.

## Estado actual

Primera fase:

- `auth-service` creado.
- `cultivos-service` creado.
- `api-gateway` creado para enrutar auth y cultivos.
- `sensores-service` creado para MQTT, estado vivo y ultima lectura.
- `riego-service` creado para estado, modo, programacion y riego manual.
- `reportes-service` creado para reportes PDF y estadisticas.
- `chat-service` creado para el chatbot/asistente.

El backend original no se modifica para esta migracion.
