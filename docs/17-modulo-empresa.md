# Módulo 17: Gestão de Empresas (Multi-Tenancy SaaS)

## Objetivo
Permitir a administração centralizada de todas as empresas (Tenants) que utilizam o sistema, possibilitando o cadastro de novas organizações e a criação automática de seus usuários administradores.

## Fluxo de Onboarding (Cadastro de Empresa)

Ao cadastrar uma nova Empresa, o sistema deve realizar as seguintes ações em uma única transação:
1.  **Criar o Tenant**: Salvar os dados da organização (Nome, CNPJ, etc).
2.  **Gerar Admin Inicial**: Criar um usuário com a role `ADMIN` vinculado exclusivamente a essa nova empresa.

## Segurança e Acesso
- **Quem acessa:** Apenas usuários com a role `ADMIN` vinculados ao Tenant Matriz (ID: 1) possuem permissão para gerenciar outras empresas.
- **Isolamento:** Uma empresa (Tenant 2) nunca verá a lista de outras empresas no sistema. Elas verão apenas o seu próprio perfil.

## Endpoints Principais (Super Admin)
- `GET /api/tenants`: Lista todas as empresas (apenas para a Matriz).
- `POST /api/tenants`: Cadastra uma nova empresa e seu admin inicial.
- `PUT /api/tenants/{id}/status`: Ativa ou desativa o acesso de uma empresa inteira ao sistema.

## Regras de Negócio
- Se um Tenant for marcado como `ativo = false`, todos os usuários vinculados a ele perdem o acesso ao sistema imediatamente, independentemente de suas senhas estarem corretas.
