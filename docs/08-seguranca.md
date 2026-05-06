# Módulo 8: Segurança Avançada e Auditoria

## Objetivo
Tornar o sistema corporativo altamente seguro e rastreável.

## Backend (Spring Boot)
1. **Roles e Permissions:** Dividir os acessos em `ROLE_ADMIN` e `ROLE_VENDEDOR`. Vendedores não podem acessar o Financeiro ou excluir clientes.
2. **Rate Limiting:** Implementar limites de requisições via Bucket4j para evitar ataques de Força Bruta no Login.
3. **Auditoria (JPA Envers ou EntityListeners):** Salvar "Quem modificou o que e quando". Guardar o log de criação e alteração em todas as tabelas (CreatedBy, UpdatedBy).

## Frontend (Angular 17)
1. Ocultar menus na Sidebar baseados na Role do JWT.
2. Desabilitar botões de exclusão (Ex: "Lixeira") caso o usuário não tenha permissão de ADMIN.
