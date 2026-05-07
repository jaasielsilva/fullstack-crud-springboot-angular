# Módulo 1: Autenticação JWT e Login

## Objetivo
Proteger o sistema garantindo que apenas usuários autenticados possam acessar o LexCRM, estabelecendo a base para controle de permissões.

## Backend (Spring Boot)
1. Adicionar dependência do `spring-boot-starter-security` e `java-jwt` (Auth0).
2. Criar entidade `Usuario` (Email, Username, Senha criptografada, Role, ResetToken).
3. Implementar `UserDetailsService` para buscar usuários por Login (Email) ou Username.
4. Criar filtro de autenticação para gerar o Token JWT no login (`/api/auth/login`).
5. Criar endpoints de recuperação de senha (`/api/auth/forgot-password` e `/api/auth/reset-password`).
6. Criar filtro de autorização para validar o token em cada requisição nas rotas `/api/**`.

## Frontend (Angular 17)
1. Criar componente isolado de Login (`/login`) e Recuperação de Senha (`/forgot-password`).
2. Criar `AuthService` para realizar o POST no `/api/auth/login` e salvar o token JWT no `localStorage`.
3. Criar modulo de **Configurações** para configurar o servidor de e-mail SMTP.
4. Criar um **HttpInterceptor** para injetar o header `Authorization: Bearer <token>` em todas as chamadas HTTP automaticamente.
5. Criar **Route Guards** (`CanActivate`) para proteger as rotas do sistema.
