# Módulo 8: Governança de TI, RBAC e Segurança

## Objetivo
Implementar um modelo de Governança de TI robusto, garantindo que cada colaborador acesse apenas o necessário para sua função (Princípio do Menor Privilégio) e que todas as ações críticas sejam rastreáveis.

## Matriz de Acessos (RBAC - Role Based Access Control)

| Recurso | ADMIN | GERENTE | VENDEDOR | SUPORTE |
| :--- | :---: | :---: | :---: | :---: |
| **Dashboard** | Full | Full | View Only | View Only |
| **Clientes** | CRUD | CRUD | Create/Read/Edit | Read Only |
| **Produtos** | CRUD | CRUD | Read Only | Read Only |
| **Usuários** | Full | Read Only | No Access | No Access |
| **Configurações**| Full | No Access | No Access | No Access |
| **Auditoria** | Full | Read Only | No Access | Read Only |

*CRUD = Create, Read, Update, Delete*

## Backend (Spring Boot)
1.  **Granularidade de Roles**: Expandir `UsuarioRole` para incluir `GERENTE`, `VENDEDOR` e `SUPORTE`.
2.  **Method Security**: Utilizar `@PreAuthorize` nos Controllers para validar a Role antes de executar o método.
    - Ex: `@PreAuthorize("hasRole('ADMIN')")` para exclusão de usuários.
3.  **Auditoria Nativa**: Implementar `Auditable` nas entidades para salvar `data_criacao`, `data_modificacao` e `usuario_id`.
4.  **JWT Claims**: Incluir as permissões/roles no payload do Token JWT para que o Frontend possa reagir sem chamadas extras.

## Frontend (Angular 17)
1.  **Directiva de Permissão**: Criar uma diretiva `*appHasRole="['ADMIN']"` para esconder elementos do DOM fisicamente.
2.  **Guards por Role**: Configurar as rotas para validar não só o login, mas se a Role do usuário permite acesso àquela URL específica.
3.  **Visual Read-Only**: Desabilitar campos de input e esconder botões de "Salvar" ou "Excluir" para perfis que não possuem permissão de escrita.

## Próximos Passos
- [ ] Criar novo enum de Roles.
- [ ] Implementar a lógica de interceptação de Roles no Frontend.
- [ ] Adicionar colunas de auditoria nas tabelas principais.
