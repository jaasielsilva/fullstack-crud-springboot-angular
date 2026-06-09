# syntax=docker/dockerfile:1.7
# ============================================================
# Stage 1: Build Angular
# ============================================================
FROM node:26-alpine AS build

WORKDIR /app

# Copia manifestos primeiro para aproveitar cache do npm
COPY package*.json ./
RUN npm ci --no-audit --no-fund

# Copia o restante e builda
COPY . .

# Configuração escolhida em build-time: production (default) ou hml
ARG BUILD_CONFIG=production
RUN npx ng build --configuration=${BUILD_CONFIG}

# ============================================================
# Stage 2: Nginx servindo o bundle estático
# ============================================================
FROM nginx:1.27-alpine

# curl para HEALTHCHECK (wget não vem garantido na imagem slim)
RUN apk add --no-cache curl

# Remove configuração default e copia a nossa
RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/app.conf

# O Angular 17 com builder application gera estrutura {browser,server,...}
# Servimos apenas a pasta browser para SPA estática
COPY --from=build /app/dist/clientes-front-v2/browser /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD curl -fsS http://127.0.0.1/ -o /dev/null || exit 1
