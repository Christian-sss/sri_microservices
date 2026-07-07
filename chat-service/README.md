# chat-service

Microservicio del chatbot/asistente.

## Rutas

- `POST /api/chat`

## Body

```json
{
  "mensaje": "Como debo regar maiz?"
}
```

## Variables de entorno

- `PORT`
- `SPRING_AI_OPENAI_BASE_URL`
- `SPRING_AI_OPENAI_API_KEY`
- `SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL`
