# Módulo 14: Monitoramento e Logs

## Objetivo
Garantir a visibilidade sobre a saúde da aplicação e agilizar a resolução de problemas técnicos.

## Logs da Aplicação

### Visualização em Desenvolvimento
Os logs são exibidos diretamente no console do terminal onde o Spring Boot ou Angular está rodando.

### Visualização em Produção (Docker)
Para visualizar os logs dos containers:
```bash
docker-compose logs -f --tail 100 lex-api
```

## Monitoramento de Saúde (Health Check)
O Spring Boot Actuator está habilitado para fornecer o status da aplicação:
- **URL:** `http://localhost:8080/actuator/health`
- **Status Esperado:** `{"status": "UP"}`

## Tipos de Erros Comuns
- **Timeout de Banco:** Verificar se o container do Banco de Dados está rodando e se a rede interna do Docker está ativa.
- **Out of Memory:** Verificar os limites de memória configurados no `docker-compose.yml` para a JVM.

## Ferramentas Recomendadas (Opcional)
Para ambientes de alta escala, recomenda-se integrar:
1. **Prometheus & Grafana:** Para métricas de performance.
2. **Sentry:** Para captura de erros em tempo real no Frontend e Backend.
3. **ELK Stack:** Para centralização de logs complexos.
