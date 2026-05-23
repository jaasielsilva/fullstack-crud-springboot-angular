# Datadog → Webhook → API → Telegram

Fluxo: **Datadog (monitor)** envia JSON para `POST /api/webhooks/datadog`; a API valida o header `X-Datadog-Token`, normaliza o payload e chama a **Bot API** do Telegram. O destino é o **chat privado do super administrador** (`TELEGRAM_CHAT_ID` obtido com o bot @userinfobot ou @getidsbot).

## Variáveis de ambiente

| Variável | Obrigatório | Descrição |
|----------|-------------|-----------|
| `DATADOG_WEBHOOK_SECRET` | Sim (produção) | Mesmo valor enviado no header `X-Datadog-Token`. |
| `TELEGRAM_BOT_TOKEN` | Sim para enviar | Token do BotFather. |
| `TELEGRAM_CHAT_ID` | Sim para enviar | ID numérico do chat (super admin). |
| `DATADOG_WEBHOOK_ALLOWED_IPS` | Não | CSV de IPs permitidos (ex.: IP público do Nginx). Vazio = qualquer IP (proteção só pelo token). |
| `DATADOG_WEBHOOK_LOG_PAYLOAD_PREVIEW` | Não | `true` loga preview do body (só debug). |
| `TELEGRAM_CONNECT_TIMEOUT_MS` | Não | Padrão 2000. |
| `TELEGRAM_SEND_TIMEOUT_MS` | Não | Padrão 5000. |

Se `DATADOG_WEBHOOK_SECRET` estiver vazio, a API responde **401** `webhook_not_configured`. Se o Telegram não estiver configurado, o webhook ainda responde **200** e o erro fica só no log.

## URL do webhook

Use a URL pública HTTPS da API (mesmo prefixo dos outros webhooks):

```text
https://api.SEU_DOMINIO/api/webhooks/datadog
```

Exemplo homologação: `https://api.dev.erpcorporativo.shop/api/webhooks/datadog`

## Exemplo `curl` (simulação Datadog)

Substitua `SEU_SEGREDO` pelo valor de `DATADOG_WEBHOOK_SECRET`:

```bash
curl -sS -X POST "https://api.dev.erpcorporativo.shop/api/webhooks/datadog" \
  -H "Content-Type: application/json" \
  -H "X-Datadog-Token: SEU_SEGREDO" \
  -d '{
    "title": "High error rate",
    "text": "More than 5 errors in 1 minute",
    "alert_type": "error",
    "date": 1710000000,
    "service": "erp-api",
    "endpoint": "/pagamentos"
  }'
```

Resposta esperada: `{"status":"processed"}`.

Corpo típico enviado ao Telegram (exemplo):

```text
🚨 ALERTA DATADOG

Título: High error rate
Status: ALERT
Serviço: erp-api
Mensagem: More than 5 errors in 1 minute
Endpoint: /pagamentos
Hora: 2024-03-09 13:00
```

Recuperação (`alert_type: success` ou texto de recovery):

```text
🟢 RECUPERADO

Título: Monitor recovered
Serviço: erp-api
Mensagem: All checks passed
Hora: ...
```

Token inválido ou ausente: **401** com `{"error":"unauthorized"}` ou `{"error":"webhook_not_configured"}`.

JSON inválido: **400** `{"error":"invalid_json"}`.

## Monitors sugeridos no Datadog

### 1. Taxa de erro HTTP 500

- **Monitors → New Monitor → Metric**.
- Métrica: trace `http.server.request` ou métrica APM custom (ex.: contagem por `http.status_code:500`).
- Threshold: alerta quando taxa ou contagem ultrapassar o limite em 5 minutos.

### 2. CPU

- **Infrastructure → Hosts** ou métrica `system.cpu.user` + `system.cpu.system`.
- Alerta quando média &gt; 85% por janela de 15 min (ajuste ao seu host).

### 3. Latência

- APM: `trace.http.server.request.duration` p95 ou métrica `http.server.requests` (Spring Boot Actuator + Micrometer exportados ao Datadog, se configurado).

## Integração “Webhook” no monitor

1. No monitor, em **Say what’s happening**, edite a notificação.
2. Adicione canal **Webhooks** (Integrations → Webhooks, ou notificação por webhook customizado).
3. **URL**: `https://api.../api/webhooks/datadog`
4. **Custom Headers**: `X-Datadog-Token: <mesmo valor que DATADOG_WEBHOOK_SECRET>`
5. **Payload**: use o template padrão do Datadog (JSON) ou um corpo mínimo com `title`, `text`, `alert_type`, `date` — campos extras são ignorados com segurança.

Para **Custom Webhook** no Datadog, garanta `Content-Type: application/json`.

## Segurança

- O endpoint está anotado com `@Hidden` e **não** aparece no Swagger UI público.
- Proteção principal: segredo compartilhado no header.
- Opcional: `DATADOG_WEBHOOK_ALLOWED_IPS` (atenção a `X-Forwarded-For` atrás do Nginx).

## Referência de código

- `DatadogWebhookController` — validação, IP opcional, parse JSON.
- `AlertService` — normalização e formatação da mensagem.
- `TelegramService` — `POST` para `api.telegram.org/bot.../sendMessage`.
