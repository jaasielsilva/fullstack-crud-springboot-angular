# GMUD — Operação e deploy

Gestão de mudanças (ITIL básico) para rastrear deploys HML/PROD integrados ao GitHub Actions.

## Acesso

- **UI Angular:** `/gmud` — somente **super admin** (`ADMIN` + `tenantId = 1`).
- **API REST:** `/api/gmud/changes` — mesmo critério (JWT).
- **CI/CD:** `/api/internal/gmud/deploy-events/*` — header `X-Deploy-Token`.

## Variáveis

| Variável | Onde | Descrição |
|----------|------|-----------|
| `GMUD_DEPLOY_TOKEN` | GitHub Secret + VPS `.env` | Token compartilhado para pipeline |
| `GMUD_ENABLED` | VPS `.env` | `true` (default) desliga integração se `false` |

## Fluxo HML (automático)

1. Pipeline `deploy-hml` chama `POST .../deploy-events/start` → GMUD `IN_APPROVAL` → auto `APPROVED`.
2. Após `docker compose up`, chama `.../success` → `DEPLOYED`.
3. Se job falhar, chama `.../failure` → `ROLLBACK` quando aplicável.

## Fluxo PROD (aprovação manual)

1. Pipeline valida se o commit possui **tag com GitHub Release**; sem release, o job falha.
2. Pipeline chama `start` → GMUD fica em `IN_APPROVAL`.
3. Super admin aprova na UI (`POST /api/gmud/changes/{id}/approve`) → `APPROVED`.
4. O job de deploy só continua se a GMUD vier como `APPROVED`; caso contrário falha com aviso no Telegram.
5. Pipeline `success` marca `DEPLOYED`.
6. Falha de deploy → `failure` → `ROLLBACK`.

## Falha do curl GMUD

- **HML (`deploy-hml`)**: deploy **não é interrompido**; o workflow usa `continue-on-error: true` e envia aviso no Telegram.
- **PROD (`deploy-prod`)**: GMUD faz parte do gate obrigatório; erro de registro/validação bloqueia o deploy.

## Health / rollback

- Health da API: `GET /actuator/health` (público).
- Rollback de release: status `ROLLBACK` na GMUD + procedimento operacional de imagem anterior no Compose (ver [09-docker-deploy.md](09-docker-deploy.md)).

## Blue/green

Nesta entrega apenas documentação: [deploy/nginx/blue-green-upstream.conf.example](../deploy/nginx/blue-green-upstream.conf.example). Portas 9101–9104 inalteradas.

## SQL em produção

Antes do primeiro deploy com `JPA_DDL_AUTO=validate`, executar:

`deploy/sql/V1__gmud_tables.sql`

## Governança v2.0

Para o modelo empresarial completo (RACI, playbook, checklists e KPIs), consulte:

- [22-gmud-governanca-empresarial-v2.md](22-gmud-governanca-empresarial-v2.md)
- [23-gmud-playbook-operacional-v2.md](23-gmud-playbook-operacional-v2.md)
- [24-gmud-checklists-e-templates-v2.md](24-gmud-checklists-e-templates-v2.md)
- [25-gmud-kpis-e-metas-v2.md](25-gmud-kpis-e-metas-v2.md)
