# Infra & Deploy — `erpcorporativo.shop`

Este diretório descreve como rodar a aplicação em uma VPS que **já tenha Nginx escutando nas portas 80 e 443** (caso típico: outros projetos como `lavarapido` no mesmo servidor).

Os containers LexCRM **não ocupam 80/443 no host**. Eles ficam bindados só em **loopback** (`127.0.0.1`) em portas altas; o **Nginx do sistema** faz `proxy_pass` para essas portas e termina o HTTPS com Let's Encrypt (Certbot).

```
deploy/
├── nginx/          # Virtual hosts de exemplo + README (copiar para /etc/nginx)
├── hml/              # Stack homologação (branch dev)
├── prod/             # Stack produção (branch main)
└── README.md         # este arquivo
```

| Ambiente | Frontend | API | Branch que dispara o deploy |
|---|---|---|---|
| Produção | `https://erpcorporativo.shop` (+ `www`) | `https://api.erpcorporativo.shop` | `main` |
| Homologação | `https://dev.erpcorporativo.shop` | `https://api.dev.erpcorporativo.shop` | `dev` |

### Portas no host (convênção deste projeto)

| Serviço Docker | Bind | Uso |
|---|---|---|
| `api-hml` | `127.0.0.1:9101` | API homologação |
| `web-hml` | `127.0.0.1:9102` | Front homologação |
| `api-prod` | `127.0.0.1:9103` | API produção |
| `web-prod` | `127.0.0.1:9104` | Front produção |

**Não conflita** com apps que expõem `:8080`, `:8081`, `:3306`, etc. — são faixas diferentes.

---

## 1) DNS

No painel do registrador (Hostinger), registros **A** para o IP da VPS:

| Host | Observação |
|---|---|
| `@` | apex `erpcorporativo.shop` |
| `www` | ou CNAME `www` → apex (como você já tem) |
| `api` | `api.erpcorporativo.shop` |
| `dev` | `dev.erpcorporativo.shop` |
| `api.dev` | `api.dev.erpcorporativo.shop` |

Não é necessário registro `traefik` se você não for usar Traefik.

---

## 2) Preparação da VPS

Docker + Compose já instalados na sua máquina — ótimo.

```bash
mkdir -p /opt/erpcorporativo/{hml,prod}
```

Chave SSH para GitHub Actions (pública em `authorized_keys`, privada no secret `VPS_SSH_KEY`) — já coberto na sua sessão.

---

## 3) Nginx no host (uma vez)

Copie o arquivo de exemplo e habilite o site:

```bash
cd /opt/erpcorporativo
git clone https://github.com/<SEU-USUARIO>/<SEU-REPO>.git repo

sudo cp repo/deploy/nginx/sites-available/erpcorporativo.shop.conf \
        /etc/nginx/sites-available/erpcorporativo.shop.conf
sudo ln -sf /etc/nginx/sites-available/erpcorporativo.shop.conf /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

Emita o certificado (domínios devem já resolver para esta VPS):

```bash
sudo certbot --nginx \
  -d erpcorporativo.shop \
  -d www.erpcorporativo.shop \
  -d api.erpcorporativo.shop \
  -d dev.erpcorporativo.shop \
  -d api.dev.erpcorporativo.shop
```

Detalhes e limpeza de configs antigas (`adegaflow`): veja [`deploy/nginx/README.md`](nginx/README.md).

---

## 4) Subir stacks Docker (hml e prod)

```bash
# HOMOLOGAÇÃO
cp /opt/erpcorporativo/repo/deploy/hml/docker-compose.yml /opt/erpcorporativo/hml/
cd /opt/erpcorporativo/hml
cp /opt/erpcorporativo/repo/deploy/hml/.env.example .env
nano .env
# Troque OWNER por jaasielsilva (ou seu org) em API_IMAGE / WEB_IMAGE
# Senhas fortes: openssl rand -base64 24

echo "<PAT read:packages>" | docker login ghcr.io -u jaasielsilva --password-stdin
docker compose pull && docker compose up -d

# PRODUÇÃO (senhas diferentes!)
cp /opt/erpcorporativo/repo/deploy/prod/docker-compose.yml /opt/erpcorporativo/prod/
cd /opt/erpcorporativo/prod
cp /opt/erpcorporativo/repo/deploy/prod/.env.example .env
nano .env
docker compose pull && docker compose up -d
```

Confirme que as portas locais respondem:

```bash
curl -sS -o /dev/null -w "%{http_code}" http://127.0.0.1:9102/
curl -sS -o /dev/null -w "%{http_code}" http://127.0.0.1:9101/actuator/health
```

---

## 5) CI/CD no GitHub

### Secrets

| Secret | Conteúdo |
|---|---|
| `TELEGRAM_BOT_TOKEN` | Token do BotFather |
| `TELEGRAM_CHAT_ID` | ID do chat |
| `VPS_HOST` | IP da VPS |
| `VPS_USER` | `root` (ou usuário com Docker) |
| `VPS_SSH_KEY` | Chave privada OpenSSH completa |

### GHCR

Depois do primeiro `Deploy HML`, em **Packages** no GitHub, libere o repositório com **Write** nos pacotes `erpcorporativo-api` e `erpcorporativo-web`.

---

## 6) Fluxo

Push em `dev` → pipeline builda imagens `:hml`, faz SSH na VPS, `docker compose pull && up -d` em `/opt/erpcorporativo/hml`.

Push em `main` → idem com `:prod` em `/opt/erpcorporativo/prod`.

Telegram notifica início e resultado.

---

## 7) Operação

```bash
cd /opt/erpcorporativo/hml && docker compose logs -f api-hml
cd /opt/erpcorporativo/prod && docker compose logs -f api-prod
```

Renovação SSL: o **Certbot** costuma instalar timer systemd (`certbot.timer`) — verifique com `systemctl list-timers | grep certbot`.

---

## 8) Solução de problemas

| Sintoma | Causa provável |
|---|---|
| `502 Bad Gateway` | Container parado ou porta errada — `docker ps`, `curl 127.0.0.1:910x` |
| `502` só pelo domínio | Nginx não recarregado ou `proxy_pass` apontando para porta errada |
| Certificado inválido | DNS não apontava para o IP na hora do `certbot` — rode `certbot renew --dry-run` |
| CORS | `APP_CORS_ALLOWED_ORIGINS` no `.env` do compose |
