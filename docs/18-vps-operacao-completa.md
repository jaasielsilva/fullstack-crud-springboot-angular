# Módulo 18: Operação na VPS — repositório, pastas, deploy e acesso

Este documento reúne o que você precisa para operar o LexCRM em produção/homologação na VPS, integrado ao GitHub e ao domínio `erpcorporativo.shop`.

---

## 1. Repositório no GitHub

| Item | Valor |
|---|---|
| **URL** | `https://github.com/jaasielsilva/fullstack-crud-springboot-angular` |
| **Clone (HTTPS)** | `git clone https://github.com/jaasielsilva/fullstack-crud-springboot-angular.git` |
| **Branch principal** | `main` — código estável, deploy de **produção** |
| **Branch de homologação** | `dev` — ambiente de testes antes do merge em `main` |

Fluxo sugerido: desenvolver em `dev` → validar em **https://dev.erpcorporativo.shop** → merge em `main` → validar em **https://erpcorporativo.shop**.

---

## 2. Estrutura de pastas no seu computador (repositório)

```
FullStack/
├── backend/clientes-api/          # API Spring Boot (Maven, Dockerfile)
├── frontend/clientes-front-v2/    # Angular (Dockerfile + Nginx no container)
├── deploy/                        # Compose da VPS + Nginx de exemplo
│   ├── hml/                       # Stack homologação
│   ├── prod/                      # Stack produção
│   ├── nginx/                     # Virtual host para copiar em /etc/nginx
│   └── README.md                  # Guia técnico detalhado
├── .github/
│   ├── workflows/                 # CI + Deploy HML + Deploy PROD
│   └── actions/telegram-notify/   # Notificações Telegram
└── docs/                          # Documentação funcional e operação
```

Documentação complementar de infra: [`deploy/README.md`](../deploy/README.md) e [`deploy/nginx/README.md`](../deploy/nginx/README.md).

---

## 3. Onde fica tudo na VPS

Base usada neste projeto: **`/opt/erpcorporativo`**

| Caminho | Conteúdo |
|---|---|
| `/opt/erpcorporativo/repo/` | Clone do GitHub (branch `dev` ou `main` conforme você der checkout). Serve para **ler** `deploy/` e atualizar com `git pull`. |
| `/opt/erpcorporativo/hml/` | **Homologação**: `docker-compose.yml` + `.env` + dados do stack HML. |
| `/opt/erpcorporativo/prod/` | **Produção**: idem para prod (quando subir). |
| `/etc/nginx/sites-available/erpcorporativo.shop.conf` | Virtual hosts HTTPS para os domínios LexCRM (proxy para portas locais). |
| `/etc/letsencrypt/live/erpcorporativo.shop/` | Certificados SSL (Let's Encrypt). |

**Imagens Docker** ficam no cache local do Docker na VPS (não é pasta única fácil de listar). Os **volumes nomeados** dos MySQL ficam gerenciados pelo Docker (ex.: `mysql_hml_data`, `mysql_prod_data`).

---

## 4. Portas e domínios

O **Nginx do sistema** usa **80 e 443** (já existiam na sua VPS). Os containers LexCRM **não** abrem essas portas no mundo; ficam só em **loopback**:

| Ambiente | Serviço | Host | Domínio público |
|---|---|---|---|
| HML | API | `127.0.0.1:9101` | https://api.dev.erpcorporativo.shop |
| HML | Front | `127.0.0.1:9102` | https://dev.erpcorporativo.shop |
| PROD | API | `127.0.0.1:9103` | https://api.erpcorporativo.shop |
| PROD | Front | `127.0.0.1:9104` | https://erpcorporativo.shop (+ www) |

Outros projetos na mesma VPS (ex.: lavarapido em `:8080`, `:8081`) **não entram em conflito** com essas portas.

---

## 5. Como funciona o CI/CD (GitHub Actions)

| Evento | Workflow | O que faz |
|---|---|---|
| Push ou PR em `dev` ou `main` | **CI** | Build backend (`mvn verify`) + build frontend (`ng build`). Notifica Telegram início/fim. |
| Push em **`dev`** | **Deploy HML** | Build imagens Docker → push **GHCR** com tags `:hml` e `:hml-<sha>` → SSH na VPS → `docker compose pull && up -d` em `/opt/erpcorporativo/hml`. |
| Push em **`main`** | **Deploy PROD** | Idem com tags `:prod` em `/opt/erpcorporativo/prod`. |

Imagens publicadas (ajuste o usuário se mudar o dono do repo):

- `ghcr.io/jaasielsilva/erpcorporativo-api:hml` / `:prod`
- `ghcr.io/jaasielsilva/erpcorporativo-web:hml` / `:prod`

### Secrets obrigatórios no GitHub (Settings → Secrets → Actions)

- `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY` — deploy por SSH  
- `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID` — avisos no Telegram  

O `GITHUB_TOKEN` é injetado automaticamente para login no GHCR durante o job.

### GHCR — primeira vez

Em **Packages** no GitHub, libere o repositório com permissão **Write** nos pacotes `erpcorporativo-api` e `erpcorporativo-web`, para o workflow conseguir dar `docker push`.

Na VPS, para `docker pull` manual ou primeira subida, use um **PAT** com escopo `read:packages` e `docker login ghcr.io`.

---

## 6. Arquivos sensíveis na VPS

Edite **somente** nestes arquivos (nunca commitar senhas no Git):

- `/opt/erpcorporativo/hml/.env`
- `/opt/erpcorporativo/prod/.env`

Contêm: senhas MySQL, `JWT_SECRET`, URLs das imagens GHCR, opcionalmente SMTP (`MAIL_*`). Veja os exemplos em `deploy/hml/.env.example` e `deploy/prod/.env.example`.

---

## 7. Primeiro acesso (super admin)

Na **primeira subida** com banco **vazio**, o `DataInitializer` cria:

| Campo | Valor padrão |
|---|---|
| Login (e-mail) | `admin@lexcrm.com.br` |
| Senha | `123` |
| Username | `admin.lex` |

**Altere a senha** assim que possível, principalmente em produção.

Se o banco já tinha usuários (restore/migração), esse usuário pode não ser criado — consulte os logs da API ou a tabela `usuarios` no MySQL.

---

## 8. E-mail (SMTP)

- Configuração **na aplicação** (tela Configurações) grava no banco e é usada pelo envio real de e-mails.
- O endpoint `/actuator/health` não deve ficar `DOWN` por falta de SMTP: por padrão `management.health.mail.enabled=false`. Para monitorar SMTP pelo Actuator depois de configurar credenciais no `.env`, use `MAIL_HEALTH_ENABLED=true` e variáveis `MAIL_*` — ver [`application.properties`](../backend/clientes-api/src/main/resources/application.properties).

---

## 9. Comandos úteis na VPS

```bash
# Atualizar só o código de referência (compose/nginx)
cd /opt/erpcorporativo/repo && git pull

# Homologação — status e logs
cd /opt/erpcorporativo/hml
docker compose ps
docker compose logs -f api-hml

# Produção (quando existir)
cd /opt/erpcorporativo/prod
docker compose ps

# Testar API localmente
curl -s http://127.0.0.1:9101/actuator/health

# Recarregar Nginx após editar site
sudo nginx -t && sudo systemctl reload nginx
```

---

## 10. Documentação relacionada

| Documento | Assunto |
|---|---|
| [`deploy/README.md`](../deploy/README.md) | DNS, VPS, stacks, troubleshooting |
| [`deploy/nginx/README.md`](../deploy/nginx/README.md) | Nginx + Certbot na VPS com outros sites |
| [`README.md`](../README.md) (raiz) | Visão geral do projeto e link para deploy |
| [`21-gmud-operacao.md`](21-gmud-operacao.md) | Operação GMUD no dia a dia |
| [`22-gmud-governanca-empresarial-v2.md`](22-gmud-governanca-empresarial-v2.md) | Governança corporativa de mudança |
| [`23-gmud-playbook-operacional-v2.md`](23-gmud-playbook-operacional-v2.md) | Playbook operacional ponta a ponta |
| [`24-gmud-checklists-e-templates-v2.md`](24-gmud-checklists-e-templates-v2.md) | Checklists e templates oficiais |
| [`25-gmud-kpis-e-metas-v2.md`](25-gmud-kpis-e-metas-v2.md) | KPIs, metas e revisão mensal |

---

## 11. Resumo em uma frase

**Código no GitHub** (`dev` / `main`) → **GitHub Actions** builda e envia imagens ao **GHCR** → **SSH na VPS** atualiza os containers em **`/opt/erpcorporativo/hml`** ou **`prod`** → **Nginx** na porta 443 encaminha para **`127.0.0.1:9101–9104`** → usuários acessam pelos **domínios** em HTTPS.
