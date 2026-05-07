# Módulo 9: Docker e Pipeline de Deploy

## Objetivo
Preparar o sistema para produção, garantindo que ele rode em qualquer servidor em nuvem da mesma forma que roda localmente.

## Implementação atual (LexCRM)

O projeto inclui **Dockerfiles**, **docker-compose** para homologação e produção, **GitHub Actions** (CI + deploy), **Nginx na VPS** (TLS no host) e documentação operacional.

**Guia completo de operação na VPS** (repositório, pastas, domínios, secrets, credenciais iniciais): ver **[Módulo 18 — VPS e repositório](18-vps-operacao-completa.md)**.

**Detalhes técnicos de infra**: [`deploy/README.md`](../deploy/README.md).

---

## Infraestrutura (visão original)

1. **Frontend (Angular):** Multi-stage build → Nginx servindo estáticos.
2. **Backend (Spring Boot):** Dockerfile JDK 17, layers Maven.
3. **Database:** MySQL em container por ambiente (hml / prod isolados).
4. **Proxy:** Nginx no host (80/443) + Let's Encrypt; containers só em `127.0.0.1`.

