# Módulo 1: Autenticação JWT e Login

## Objetivo
Proteger o sistema garantindo que apenas usuários autenticados possam acessar o LexCRM, estabelecendo a base para controle de permissões.

## Backend (Spring Boot)
1. Adicionar dependência do `spring-boot-starter-security` e `java-jwt` (Auth0).
2. Criar entidade `Usuario` (Email, Senha criptografada, Role).
3. Implementar `UserDetailsService` para buscar usuários no banco.
4. Criar filtro de autenticação para gerar o Token JWT no login (`/api/auth/login`).
5. Criar filtro de autorização para validar o token em cada requisição nas rotas `/api/**`.

## Frontend (Angular 17)
1. Criar componente isolado de Login (`/login`) fora do `app.component` (sem sidebar).
2. Criar `AuthService` para realizar o POST no `/api/auth/login` e salvar o token JWT no `localStorage`.
3. Criar um **HttpInterceptor** para injetar o header `Authorization: Bearer <token>` em todas as chamadas HTTP automaticamente.
4. Criar **Route Guards** (`CanActivate`) para impedir que usuários não logados acessem rotas como `/clientes` ou `/dashboard`.
