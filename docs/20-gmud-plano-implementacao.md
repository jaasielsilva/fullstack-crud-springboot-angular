# Plano de implementação — GMUD + DevOps (LexCRM)

Documento executável no repositório. Marque `- [x]` conforme concluir cada item.

**Decisões fixas:** PROD exige aprovação manual; HML automático; falha curl GMUD só avisa no Telegram; UI só super admin (`ADMIN` + `tenantId=1`); blue/green só documentação.

---

## Fase 0 — Documentação

- [x] Criar `docs/20-gmud-plano-implementacao.md` (este arquivo)
- [x] Criar `docs/21-gmud-operacao.md`
- [x] Link no `README.md`

---

## Fase 1 — Backend GMUD

### 1.1 Enums
- [x] `ChangeType`, `ChangeStatus`, `DeployEnvironment`, `RiskLevel`

### 1.2 Entidades
- [x] `ChangeRequest` → `change_requests`
- [x] `ChangeLog` → `change_logs`

### 1.3 Repositórios
- [x] `ChangeRequestRepository`
- [x] `ChangeLogRepository`

### 1.4 DTOs
- [x] `CreateChangeRequestDTO`, `ChangeRequestResponseDTO`, `ChangeLogResponseDTO`
- [x] `StatusTransitionDTO`, `CicdDeployEventDTO`, `CicdDeployResponseDTO`

### 1.5 Services
- [x] `ChangeRequestService`
- [x] `ChangeRequestCicdService`
- [x] `SuperAdminSupport`

### 1.6 Controllers
- [x] `ChangeRequestController` — `/api/gmud/changes`
- [x] `ChangeRequestCicdController` — `/api/internal/gmud/deploy-events`

### 1.7 Segurança
- [x] `DeployTokenFilter`
- [x] `SecurityConfig` — permit internal + filtro
- [x] `application.properties` — `app.gmud.*`
- [x] `application.properties` (test)

### 1.8 DDL
- [x] `deploy/sql/V1__gmud_tables.sql`

### 1.9 Testes
- [x] `ChangeRequestServiceTest`
- [x] `ChangeRequestCicdServiceTest`

**Critério:** `cd backend/clientes-api && ./mvnw clean verify`

---

## Fase 2 — Frontend Angular

- [x] Models `change-request`, `change-log`
- [x] `gmud.service.ts`
- [x] `gmud-list`, `gmud-form`, `gmud-detail`
- [x] Rotas em `app.routes.ts` + `adminGuard`
- [x] Menu em `app.component.html`

**Critério:** `ng build --configuration=production`

---

## Fase 3 — CI/CD

- [x] Secret `GMUD_DEPLOY_TOKEN` documentado
- [x] `deploy-hml.yml` — steps GMUD + Telegram em falha
- [x] `deploy-prod.yml` — idem
- [x] `deploy/hml/docker-compose.yml` + `.env.example`
- [x] `deploy/prod/docker-compose.yml` + `.env.example`

---

## Fase 4 — DevOps documentação

- [x] `deploy/nginx/blue-green-upstream.conf.example`
- [x] `deploy/nginx/README.md` — referência
- [x] `docs/09-docker-deploy.md` — link GMUD

---

## Fase 5 — Qualidade

- [x] `mvn clean verify`
- [x] `ng build --configuration=production`
- [ ] Smoke: super admin → `/gmud` → criar → aprovar (manual com API local)
- [ ] Smoke: curl CI com `X-Deploy-Token` (após configurar secret em HML/PROD)

---

## Ordem de execução

1. Fase 0 → 1 → 2 → 3 → 4 → 5

## Referências

- Operação: [21-gmud-operacao.md](21-gmud-operacao.md)
- API Swagger: `/swagger-ui` tag **GMUD**
