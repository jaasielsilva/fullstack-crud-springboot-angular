# Nginx no servidor (quando 80/443 já estão em uso)

Nesta VPS o **Nginx do sistema** já escuta **80 e 443** (ex.: `lavarapido` / outros sites).  
Por isso **não usamos Traefik** aqui: os containers LexCRM expõem apenas **loopback** (`127.0.0.1`) em portas altas, e o Nginx faz `proxy_pass` para elas.

## Mapa de portas internas

| Serviço no Docker | Bind no host | Domínio no Nginx |
|---|---|---|
| `api-hml` | `127.0.0.1:9101` | `api.dev.erpcorporativo.shop` |
| `web-hml` | `127.0.0.1:9102` | `dev.erpcorporativo.shop` |
| `api-prod` | `127.0.0.1:9103` | `api.erpcorporativo.shop` |
| `web-prod` | `127.0.0.1:9104` | `erpcorporativo.shop` + `www` |

**Não conflita** com `lavarapido` em `:8080` / `:8081` / MySQL em `:3306` — são portas diferentes.

## Blue/Green (preparação)

Exemplo de configuração dual-upstream para troca sem downtime de proxy: [`blue-green-upstream.conf.example`](blue-green-upstream.conf.example).

**Nesta entrega:** não alterar portas na VPS; deploy continua **rolling** via Docker Compose. Ver também [docs/20-gmud-plano-implementacao.md](../../docs/20-gmud-plano-implementacao.md) e [docs/21-gmud-operacao.md](../../docs/21-gmud-operacao.md).

## Instalar o site no Nginx

```bash
sudo cp /opt/erpcorporativo/repo/deploy/nginx/sites-available/erpcorporativo.shop.conf \
        /etc/nginx/sites-available/erpcorporativo.shop.conf

sudo ln -sf /etc/nginx/sites-available/erpcorporativo.shop.conf /etc/nginx/sites-enabled/

sudo nginx -t && sudo systemctl reload nginx
```

## Certificado SSL (Let's Encrypt + Certbot)

Se o `certbot` já está instalado (comum quando há outros sites com HTTPS):

```bash
sudo certbot --nginx \
  -d erpcorporativo.shop \
  -d www.erpcorporativo.shop \
  -d api.erpcorporativo.shop \
  -d dev.erpcorporativo.shop \
  -d api.dev.erpcorporativo.shop
```

O Certbot ajusta os blocos `listen 443 ssl` e os caminhos `ssl_certificate` nos arquivos.

## Limpar configs antigas (opcional)

Se ainda existirem symlinks do projeto **adegaflow** em `/etc/nginx/sites-enabled/` e você não usa mais:

```bash
ls -la /etc/nginx/sites-enabled/
sudo rm /etc/nginx/sites-enabled/adegaflow   # se existir
sudo nginx -t && sudo systemctl reload nginx
```

No painel DNS da Hostinger você pode **apagar** o registro `traefik` se tinha criado só para o Traefik — não é mais necessário.

## Rede Docker `web`

Se você criou `docker network create web` pensando no Traefik, **não é mais necessária** para o LexCRM (os compose usam rede bridge interna). Pode deixar lá sem problema ou remover quando não houver container usando.
