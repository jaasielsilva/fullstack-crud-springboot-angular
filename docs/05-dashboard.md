# Módulo 5: Dashboard e Gráficos

## Objetivo
Evoluir o Dashboard atual para uma verdadeira central de inteligência do negócio.

## Frontend (Angular 17)
1. Instalar biblioteca de gráficos (ex: `Chart.js` via `ng2-charts` ou `ngx-echarts`).
2. Implementar os seguintes painéis:
   - **Gráfico de Barras:** Faturamento nos últimos 6 meses.
   - **Gráfico de Pizza:** Produtos mais vendidos.
   - **Cards KPI:** Faturamento do Mês, Ticket Médio, Novos Clientes.

## Backend (Spring Boot)
1. Criar endpoints consolidados no `DashboardController` para retornar os dados já somados e agrupados (para não sobrecarregar o Front-end processando milhares de pedidos).
