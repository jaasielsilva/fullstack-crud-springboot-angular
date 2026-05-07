# Módulo 16: Arquitetura Multi-Tenant

## Objetivo
Permitir que múltiplas empresas (Tenants) utilizem o LexCRM de forma isolada, garantindo que os dados de uma organização nunca sejam acessíveis por outra, mesmo compartilhando o mesmo banco de dados.

## Estratégia de Implementação: Discriminador de Dados (Shared Database)
Utilizaremos uma coluna `tenant_id` em todas as tabelas críticas para filtrar os dados dinamicamente.

### 1. Entidade Tenant
- Representa a empresa/organização contratante.
- Atributos: `id`, `nome`, `cnpj`, `ativo`.

### 2. Isolamento de Dados
- Todas as entidades herdam de `AuditModel`, que agora inclui a coluna `tenant_id`.
- **Filtro Automático:** Utilizaremos o `@Filter` do Hibernate associado ao `TenantContext` para injetar o `tenant_id` em todas as queries.

### 3. Autenticação e Contexto
- O `tenant_id` é incluído como uma claim no Token JWT no momento do login.
- Um `TenantFilter` no Spring Boot extrai esse ID do token e o coloca em um `ThreadLocal` para ser usado pela camada de persistência.

## Impactos no Sistema

### Usuários
- Cada usuário pertence obrigatoriamente a um Tenant.
- O primeiro usuário criado para um Tenant recebe a role `ADMIN`.

### Clientes e Produtos
- Ao listar clientes, o sistema executa automaticamente: `SELECT * FROM clientes WHERE tenant_id = ?`.

### Configurações
- Configurações de E-mail (SMTP) e Notificações são isoladas por Tenant. Cada empresa pode ter seu próprio servidor de e-mail.

## Próximos Passos de Implementação
- [ ] Criar entidade Tenant.
- [ ] Adicionar `tenant_id` em `AuditModel`.
- [ ] Criar `TenantContext` e `TenantFilter`.
- [ ] Atualizar lógica de login para validar o Tenant.
