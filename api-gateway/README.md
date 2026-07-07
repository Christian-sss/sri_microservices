# api-gateway

Entrada unica para el frontend.

## Rutas reenviadas

- `/auth/**` hacia `auth-service`
- `/api/auth/**` hacia `auth-service`
- `/api/cultivos/**` hacia `cultivos-service`
- `/api/mqtt/**` hacia `sensores-service`
- `/api/sensor/**` hacia `sensores-service`
- `/api/estado-vivo` hacia `sensores-service`
- `/api/riego/**` hacia `riego-service`
- `/api/reportes/**` hacia `reportes-service`
- `/api/estadisticas/**` hacia `reportes-service`
- `/api/chat/**` hacia `chat-service`

## Ruta de salud

- `GET /gateway/health`

## Variables de entorno

- `PORT`
- `AUTH_SERVICE_URL`
- `CULTIVOS_SERVICE_URL`
- `SENSORES_SERVICE_URL`
- `RIEGO_SERVICE_URL`
- `REPORTES_SERVICE_URL`
- `CHAT_SERVICE_URL`
