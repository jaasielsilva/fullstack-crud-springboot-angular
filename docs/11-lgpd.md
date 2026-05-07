# Módulo 11: Governança de Dados e LGPD

## Objetivo
Garantir que o LexCRM esteja em conformidade com a Lei Geral de Proteção de Dados (Lei nº 13.709/2018), protegendo a privacidade dos clientes e usuários.

## Pilares de Conformidade no LexCRM

### 1. Transparência e Consentimento
- Todo cliente cadastrado deve ter a finalidade do uso de seus dados claramente definida.
- O sistema armazena apenas dados necessários para a operação comercial (Nome, E-mail, Telefone).

### 2. Direito ao Esquecimento (Exclusão)
- O Administrador tem o poder de excluir permanentemente os dados de um cliente a pedido do titular.
- A exclusão em cascata garante que logs de pedidos e históricos também sejam anonimizados ou removidos.

### 3. Segurança da Informação
- **Criptografia de Senhas:** Todas as senhas de usuários são protegidas com BCrypt (Hashing de via única).
- **Tokens de Acesso:** O acesso aos dados é restrito via JWT (JSON Web Tokens) com tempo de expiração curto.

### 4. Auditoria de Acesso (Rastreabilidade)
- Conforme implementado no Módulo 8, o sistema registra quem acessou ou modificou dados sensíveis, permitindo auditorias em caso de vazamentos.

## Boas Práticas para o Administrador
- Nunca compartilhar senhas de acesso.
- Atribuir o perfil de `VENDEDOR` (acesso restrito) para colaboradores que não precisam de visão gerencial.
- Realizar a limpeza periódica de leads inativos.
