# Módulo 9: Docker e Pipeline de Deploy

## Objetivo
Preparar o sistema para produção, garantindo que ele rode em qualquer servidor em nuvem (AWS, DigitalOcean) da mesma forma que roda localmente.

## Infraestrutura (Docker)
1. **Frontend (Angular):**
   - Criar `Dockerfile` usando Multi-stage build.
   - Stage 1: Fazer o `npm run build`.
   - Stage 2: Servir os arquivos estáticos usando NGINX.
2. **Backend (Spring Boot):**
   - Criar `Dockerfile` com imagem JDK 17 (Ex: Alpine).
   - Empacotar o `.jar` gerado pelo Maven.
3. **Database:**
   - Utilizar imagem oficial do MySQL.
4. **Orquestração:**
   - Criar `docker-compose.yml` unindo as 3 partes (Front, Back e DB) na mesma rede, com variáveis de ambiente injetadas dinamicamente.
