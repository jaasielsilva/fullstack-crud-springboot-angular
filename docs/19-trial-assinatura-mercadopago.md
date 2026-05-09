# Trial de 7 dias, planos e Mercado Pago — Implementação e manual operacional

Este documento descreve o que foi implementado no ERP Corporativo (SaaS multi-tenant) para trial sem cartão, planos Básico/Premium, checkout Mercado Pago, webhook, bloqueio de empresa e telas Angular relacionadas.

---

## 1. Ambientes (URLs)

| Ambiente        | Frontend                                      | Observação                          |
|-----------------|-----------------------------------------------|-------------------------------------|
| **Homologação** | `https://dev.erpcorporativo.shop`             | Ex.: login em `/login`              |
| **Produção**    | `https://erpcorporativo.shop`                 | Mesmo código; configurar env abaixo |

O backend de cada ambiente deve expor `APP_FRONTEND_URL` e as URLs de retorno do Mercado Pago (`back_urls`) derivam dessa base (Angular em `/pagamento/sucesso`, `/pagamento/falha`, `/pagamento/pendente`).

**Webhook**: cada ambiente precisa da própria `notification_url` apontando para a API correspondente (ex.: homologação `https://api-dev...` e produção `https://api.erpcorporativo.shop/...`). Nunca misturar webhook de HML apontando para API de PRD.

---

## 2. Visão da implementação

- **Empresa (tenant)**: entidade `Tenant`, tabela `tenants`, com `status` (`TRIAL`, `ATIVA`, `BLOQUEADA`, `CANCELADA`), `trialInicio`, `trialFim`, `mercadoPagoCustomerId`, contatos e documento (coluna legada `cnpj`, JSON REST usa `cnpj`).
- **Planos**: tabela `planos`, catálogo global (sem `tenant_id`), tipos `BASICO` e `PREMIUM`, valores e `recursosLiberados` (JSON em texto).
- **Assinatura**: `assinaturas` com `tenant_id`, vínculo ao plano, status (`TRIAL`, `PENDENTE`, `ATIVA`, `CANCELADA`, `EXPIRADA`), IDs Mercado Pago e `external_reference`.
- **Pagamento**: `pagamentos`, auditoria do payload MP (`payload_json`), idempotência por `mercado_pago_payment_id` quando `APPROVED`.
- **Webhook**: `webhook_eventos`, payload bruto + flag `processado`.

Fluxos principais:

1. **Cadastro trial público**: `POST /api/public/cadastro-trial` → empresa `TRIAL` + usuário `ADMIN` + assinatura `TRIAL` (sem plano pago).
2. **Checkout**: `POST /api/public/checkout` (JWT obrigatório) → cria preferência MP no backend → devolve `checkoutUrl`.
3. **Webhook**: `POST /api/webhooks/mercadopago` → persiste evento → consulta `GET /v1/payments/{id}` → atualiza pagamento/assinatura/empresa conforme status MP.

Segurança:

- Token `mercadopago.access-token` só no servidor.
- Liberação de módulos só após **approved** confirmado na API (não pela página de sucesso).
- `TenantAccessFilter` bloqueia APIs internas quando empresa está `BLOQUEADA` ou `CANCELADA`, exceto rotas permitidas (`/api/auth/me`, `/api/auth/reset-password-forced`, `/api/public/checkout`, `/api/public/planos`).
- Job agendado (`TrialService`) expira trials sem assinatura ativa válida.

---

## 3. Variáveis de ambiente (referência)

Backend (`application.properties` + overrides por env):

| Variável | Descrição |
|----------|-----------|
| `APP_FRONTEND_URL` | Base do front (HML: `https://dev.erpcorporativo.shop`, PRD: `https://erpcorporativo.shop`) |
| `MERCADO_PAGO_ACCESS_TOKEN` | Access token da aplicação MP |
| `MERCADO_PAGO_NOTIFICATION_URL` | URL pública do webhook **daquele ambiente** |
| `MERCADO_PAGO_PREFER_SANDBOX` | `true` para preferir `sandbox_init_point` em testes |
| `APP_TRIAL_VERIFICATION_CRON` | Cron Spring (padrão no properties: a cada 15 minutos) |
| `APP_CORS_ALLOWED_ORIGINS` | Incluir origem do front HML e PRD conforme deploy |

Frontend Angular: `environment.ts` / `environment.hml.ts` — `apiUrl` deve apontar para a API do mesmo ambiente.

---

## 4. Endpoints REST (resumo)

| Método | Caminho | Auth | Descrição |
|--------|---------|------|-----------|
| POST | `/api/public/cadastro-trial` | Não | Onboarding trial 7 dias |
| GET | `/api/public/planos` | Não | Lista planos ativos |
| POST | `/api/public/checkout` | Sim | Cria preferência MP |
| POST | `/api/webhooks/mercadopago` | Não | Webhook MP (produção: validar assinatura quando configurado) |
| POST | `/api/auth/login` | Não | Retorna `token` + `subscription` (snapshot) |
| GET | `/api/auth/me` | Sim | Atualiza snapshot de assinatura/status empresa |

Detalhes de DTOs e regras estão nos services Java (`TrialSignupService`, `CheckoutMercadoPagoService`, `MercadoPagoWebhookService`, `SubscriptionSnapshotService`).

---

## 5. Frontend Angular (rotas)

- `/cadastro-trial` — formulário trial.
- `/planos` — listagem e botões Assinar (chama checkout e redireciona).
- `/checkout` — redireciona para `/planos`.
- `/pagamento/sucesso`, `/pagamento/falha`, `/pagamento/pendente` — retorno MP (informativo).
- `/trial-expirado` — mensagem orientando contratação.

Guards: `authGuard` + `subscriptionGuard` (consulta `/api/auth/me` e redireciona bloqueados para `/planos`, com exceções para pagamento e `reset-password-forced`).

---

## 6. exemplos JSON Mercado Pago

Arquivo dedicado (corpo de preferência + exemplo de webhook):

- `backend/clientes-api/docs/mercadopago-preference-examples.json`

---

## 7. Operação e troubleshooting

- **Trial não bloqueia após 7 dias**: conferir job (`TrialService`), cron `app.trial.verification-cron`, logs da aplicação e `trial_fim` no banco.
- **Pagamento ok no MP mas sistema não libera**: webhook deve chegar na URL correta do ambiente; conferir `notification_url` na preferência; logs em `webhook_eventos` e `pagamentos.payload_json`; token MP válido na consulta `GET /v1/payments/{id}`.
- **403 EMPRESA_BLOQUEADA**: esperado para APIs internas; usuário deve usar `/planos` e `/api/public/checkout`.
- **CORS**: garantir origem `dev.erpcorporativo.shop` ou `erpcorporativo.shop` em `APP_CORS_ALLOWED_ORIGINS`.

---

## 8. Melhorias recomendadas para produção

- Validar **assinatura secreta** do webhook Mercado Pago (`x-signature`).
- Migrações versionadas (Flyway/Liquibase) em vez de depender só de `ddl-auto`.
- Enforcement de **recursos por plano** nos módulos (hoje `recursosLiberados` já é persistido e enviado no snapshot).

---

## 9. Índice de código (referência rápida)

| Área | Pacote / arquivo principal |
|------|----------------------------|
| Entidades | `com.clientes_api.model` (`Tenant`, `Plano`, `Assinatura`, `Pagamento`, `WebhookEvento`) |
| Serviços MP | `MercadoPagoApiService`, `CheckoutMercadoPagoService`, `MercadoPagoWebhookService` |
| Trial | `TrialSignupService`, `TrialService` |
| Segurança comercial | `TenantAccessFilter`, `SecurityConfig` |
| Controllers públicos | `PublicTrialRegistrationController`, `PublicCheckoutController`, `MercadoPagoWebhookController` |
| Snapshot login | `SubscriptionSnapshotService`, `LoginResponseDTO`, `GET /api/auth/me` |

---

*Documento gerado para alinhar homologação (`dev.erpcorporativo.shop`) e produção (`erpcorporativo.shop`) com a mesma base de código e configurações distintas por ambiente.*
