# Módulo 13: Integrações e Documentação da API

## Objetivo
Fornecer orientações para desenvolvedores e sistemas terceiros que desejam integrar-se ao ecossistema LexCRM.

## Swagger UI (Interface Visual)
A documentação interativa da API está disponível em tempo de execução:
- **URL Local:** `http://localhost:8080/swagger-ui.html`
- **Formato:** OpenAPI 3.0

## Autenticação via API
Para consumir os endpoints, é necessário enviar o Token JWT no Header da requisição:
- **Header:** `Authorization`
- **Valor:** `Bearer <seu_token_aqui>`

## Principais Endpoints

### 1. Clientes
- `GET /api/clientes`: Lista todos os clientes.
- `POST /api/clientes`: Cadastra um novo cliente.

### 2. Produtos
- `GET /api/produtos`: Catálogo de produtos.

### 3. Autenticação
- `POST /api/auth/login`: Gera o token de acesso (Credenciais: login/senha).

## Padrões de Resposta
- `200 OK`: Requisição bem-sucedida.
- `201 Created`: Registro criado com sucesso.
- `401 Unauthorized`: Token inválido ou ausente.
- `403 Forbidden`: Usuário logado mas sem permissão para esta ação (RBAC).
- `400 Bad Request`: Erro de validação nos dados enviados.
