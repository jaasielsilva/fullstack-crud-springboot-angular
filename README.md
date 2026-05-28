# LexCRM — ERP / CRM corporativo SaaS

> **LexCRM** é uma aplicação **full stack** orientada a **B2B SaaS**: API REST em **Java 17** + **Spring Boot 4**, SPA em **Angular 17**, persistência em **MySQL**, modelo **multi-tenant**, autenticação **JWT**, integração com **pagamentos (Mercado Pago)** e **deploy contínuo** em **VPS** com **Docker**, **Nginx** e **GitHub Actions**.

---

## Visão geral

O projeto cobre o ciclo típico de um produto SaaS: cadastro trial, planos, assinatura, módulos operacionais (clientes, produtos, pedidos, dashboard), gestão de usuários e empresas (tenants), configurações, além de pipelines de **CI/CD**, imagens no **GHCR** e ambientes **homologação** e **produção**.

Documentação funcional e de operação adicional está em **[`docs/`](docs/)** (JWT, multi-tenancy, segurança, LGPD, monitoramento, Mercado Pago, etc.).

---

## Stack

| Camada | Tecnologias |
|--------|-------------|
| **Backend** | Java 17, Spring Boot 4.x, Spring Web MVC, Spring Data JPA, Spring Security, JWT, Bean Validation, Lombok |
| **API** | REST, **springdoc-openapi** (Swagger UI em `/swagger-ui/**`) |
| **Frontend** | Angular 17 (standalone, rotas com guards), TypeScript, Bootstrap 5 |
| **Dados** | MySQL 8 |
| **Infra** | Docker, Docker Compose, Nginx (reverse proxy no host), VPS Linux |
| **CI/CD** | GitHub Actions (build/testes, build de imagens, deploy por SSH, notificações Telegram) |

Versão do Spring Boot conforme [`backend/clientes-api/pom.xml`](backend/clientes-api/pom.xml) (parent `spring-boot-starter-parent`).

---

## Principais módulos

### Backend — prefixos REST (`/api/...`)

| Prefixo | Descrição resumida |
|---------|-------------------|
| `/api/auth` | Login, recuperação de senha, fluxos relacionados à autenticação |
| `/api/clientes` | CRUD de clientes |
| `/api/produtos` | CRUD de produtos |
| `/api/pedidos` | Pedidos |
| `/api/dashboard` | Indicadores / dashboard |
| `/api/usuarios` | Usuários e perfis de acesso |
| `/api/tenants` | Empresas (tenants) e onboarding |
| `/api/config/email` | Configuração de e-mail |
| `/api/public` | Endpoints públicos (checkout, cadastro trial, etc.) |
| `/api/webhooks` | Webhooks (ex.: Mercado Pago) |
| `/api/gmud/changes` | GMUD — gestão de mudanças (super admin plataforma) |

Detalhes e regras de negócio: arquivos em [`docs/`](docs/) e anotações OpenAPI nos controllers.

### Frontend — páginas (`frontend/clientes-front-v2`)

- Dashboard executivo  
- Clientes, produtos, pedidos  
- Usuários, empresas (tenants)  
- Planos, fluxos de pagamento (sucesso / falha / pendente)  
- Cadastro trial, trial expirado  
- Login, esqueci senha, redefinição obrigatória de senha  
- Configurações, acesso negado  

Rotas e guards: [`frontend/clientes-front-v2/src/app/app.routes.ts`](frontend/clientes-front-v2/src/app/app.routes.ts).

---

## UX e qualidade (front)

- Layout com sidebar / topbar, grid responsivo  
- Formulários em offcanvas, skeleton loading, toasts, máscaras (ex.: telefone)  
- Backend: DTOs, exception handler global, validação  

---

## Estrutura do repositório

```
├── backend/clientes-api/          # API Spring Boot
├── frontend/clientes-front-v2/  # SPA Angular 17
├── deploy/                        # Compose HML/PROD, exemplos Nginx, guia de deploy
├── docs/                          # Documentação por tema (JWT, multi-tenant, MP, LGPD…)
├── .github/workflows/             # CI, deploy-hml, deploy-prod
└── assets/                        # Materiais auxiliares (ex.: série LinkedIn)
```

---

## Como rodar localmente

### Pré-requisitos

- Java 17+  
- Node.js 18+  
- MySQL acessível  

### Backend

1. Ajuste `backend/clientes-api/src/main/resources/application.properties` (URL/usuário/senha do MySQL e demais propriedades necessárias).  
2. Na pasta do backend:

```bash
cd backend/clientes-api
./mvnw spring-boot:run
```

API (padrão): `http://localhost:8080`  
**Swagger UI:** `http://localhost:8080/swagger-ui/index.html` (caminho pode variar conforme versão do springdoc).

### Frontend

```bash
cd frontend/clientes-front-v2
npm install
ng serve
```

Após `ng serve`, o front costuma ser `http://localhost:4200`. A API já inclui essa origem (e `127.0.0.1:4200`) no CORS padrão; só use `APP_CORS_ALLOWED_ORIGINS` se precisar de outra URL.

---

## Docker, VPS e CI/CD

A aplicação roda em **containers**; em produção/homologação o **TLS** é terminado pelo **Nginx do host**; as APIs e o front publicados no Compose fazem bind em **127.0.0.1** em portas altas, com `proxy_pass` no Nginx.

| Ambiente | Frontend | API | Branch que dispara deploy |
|----------|----------|-----|---------------------------|
| Produção | `https://erpcorporativo.shop` (+ `www`) | `https://api.erpcorporativo.shop` | `main` |
| Homologação | `https://dev.erpcorporativo.shop` | `https://api.dev.erpcorporativo.shop` | `dev` |

- **Guia completo de infra:** [`deploy/README.md`](deploy/README.md)  
- **Credenciais de bootstrap** (super admin inicial em banco vazio): mesma página, seção **9) Credenciais de bootstrap**  
- **Workflows:** `.github/workflows/ci.yml` (CI em PR/push), `deploy-hml.yml`, `deploy-prod.yml`
- **Gate obrigatório de PROD (`main`):** deploy só executa com **GitHub Release** para a tag do commit e **GMUD em `APPROVED`**; sem isso o workflow falha antes do SSH deploy.

---

## Documentação (`docs/`)

| Arquivo | Tema |
|---------|------|
| [`docs/01-jwt-login.md`](docs/01-jwt-login.md) | JWT e login |
| [`docs/16-multi-tenancy.md`](docs/16-multi-tenancy.md) | Multi-tenant |
| [`docs/08-seguranca.md`](docs/08-seguranca.md) | Segurança |
| [`docs/11-lgpd.md`](docs/11-lgpd.md) | LGPD |
| [`docs/13-api-swagger.md`](docs/13-api-swagger.md) | Swagger / OpenAPI |
| [`docs/14-monitoramento.md`](docs/14-monitoramento.md) | Monitoramento e logs |
| [`docs/19-trial-assinatura-mercadopago.md`](docs/19-trial-assinatura-mercadopago.md) | Trial, assinatura, Mercado Pago |
| [`docs/09-docker-deploy.md`](docs/09-docker-deploy.md) | Docker e deploy |
| [`docs/20-gmud-plano-implementacao.md`](docs/20-gmud-plano-implementacao.md) | Plano passo a passo — módulo GMUD / deploy |
| [`docs/21-gmud-operacao.md`](docs/21-gmud-operacao.md) | GMUD — tokens, fluxo HML/PROD, rollback |
| Demais arquivos | Produtos, pedidos, dashboard, usuários, financeiro, backup, VPS, etc. |

---

## Segurança

- Não commite `.env`, chaves JWT, senhas de banco ou chaves de API.  
- Use `APP_ADMIN_EMAIL` / `APP_ADMIN_SENHA` em ambientes reais.  
- Webhooks e endpoints públicos devem ser expostos com validação e rate limiting adequados no edge (Nginx) quando em produção.

---

## Licença

Defina a licença do repositório conforme sua necessidade (ex.: MIT, Apache-2.0, ou proprietário).
