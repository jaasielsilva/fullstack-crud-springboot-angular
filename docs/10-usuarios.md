# Módulo 10: Gestão de Usuários

## Objetivo
Permitir que administradores gerenciem (Criar, Editar, Listar e Excluir) os usuários que acessam o LexCRM, controlando seus níveis de acesso (Roles).

## Backend (Spring Boot)
1.  **Controller**: Criar `UsuarioController` para gerenciar as operações de CRUD.
2.  **Service**: Criar `UsuarioService` para lidar com a criptografia de senha e validações de e-mail duplicado.
3.  **Segurança**: Garantir que apenas usuários com `ROLE_ADMIN` possam acessar os endpoints de gestão de usuários.
4.  **Endpoints**:
    - `GET /api/usuarios`: Listar todos os usuários.
    - `POST /api/usuarios`: Criar um novo usuário.
    - `PUT /api/usuarios/{id}`: Atualizar dados ou senha.
    - `DELETE /api/usuarios/{id}`: Remover um usuário.

## Frontend (Angular 17)
1.  **Página de Usuários**: Criar componente para listar usuários em uma tabela moderna.
2.  **Modal de Cadastro**: Criar um formulário para adição e edição de usuários, permitindo escolher o e-mail, username e a Role (ADMIN ou USER).
3.  **Ações**: Implementar a exclusão e edição rápida diretamente na tabela.
4.  **Feedback**: Usar alertas para confirmar as operações.
