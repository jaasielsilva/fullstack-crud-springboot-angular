# Módulo 7: Relatórios e Exportações

## Objetivo
Permitir que a gestão extraia dados do sistema para PDF ou Excel.

## Backend (Spring Boot)
1. Integrar biblioteca `Apache POI` para exportação Excel, ou `JasperReports`/`iText` para PDFs.
2. Criar endpoints `/api/relatorios/vendas-pdf`.

## Frontend (Angular 17)
1. Criar módulo de relatórios com formulários de Filtro (Data Inicial, Data Final, Status).
2. Tratamento de download de arquivos Blob via Angular `HttpClient` para exibir um Toast ao concluir o download.
