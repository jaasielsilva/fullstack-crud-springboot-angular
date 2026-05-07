# Módulo 15: Configurações Globais do Sistema

## Objetivo
Centralizar as instruções para personalização e funcionamento de recursos transversais do LexCRM.

## 1. Configuração de E-mail (SMTP)
O LexCRM utiliza e-mail para recuperação de senhas e notificações.
- **Acesso:** Menu Configurações > Servidor de E-mail.
- **Gmail (Recomendado):**
  - **Host:** `smtp.gmail.com`
  - **Porta:** `587`
  - **TLS:** Habilitado.
  - **Senha de App:** É necessário gerar uma "Senha de App" nas configurações da sua Conta Google, pois senhas comuns são bloqueadas por segurança.

## 2. Gestão de Notificações
O Administrador pode definir quais eventos disparam alertas no sistema:
- **Novos Clientes:** Notifica quando um lead é inserido.
- **Canais:** E-mail e Notificações Push (Navegador).

## 3. Personalização (Em breve)
Espaço reservado para documentar a troca de:
- Logotipo da empresa na Sidebar.
- Esquema de cores principal (Temas).
- Nome da aplicação nos e-mails enviados.

## 4. Auditoria de Alterações
As configurações alteradas nesta tela também são auditadas (registrando `updated_by` e `updated_at` no banco de dados), garantindo governança sobre quem modificou as regras do sistema.
