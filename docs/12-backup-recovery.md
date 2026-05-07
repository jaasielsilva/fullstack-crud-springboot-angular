# Módulo 12: Backup e Recuperação de Desastres

## Objetivo
Estabelecer processos para garantir a continuidade do negócio e a integridade dos dados em caso de falhas de hardware, software ou ataques cibernéticos.

## Estratégia de Backup

### 1. Banco de Dados (PostgreSQL/MySQL)
- **Frequência:** Recomendado backup diário (Dump).
- **Ferramenta:** `pg_dump` (PostgreSQL) ou `mysqldump` (MySQL).
- **Comando de Exemplo:**
  ```bash
  docker exec lex-db pg_dump -U admin lexcrm_db > backup_$(date +%F).sql
  ```

### 2. Arquivos de Configuração
- O arquivo `.env` e as configurações de deploy (Docker Compose) devem ser armazenados em repositório privado ou cofre de senhas (Vault).

## Plano de Recuperação (Disaster Recovery)

### Passo 1: Restauração da Infraestrutura
1. Subir os containers utilizando o `docker-compose.yml`.
2. Verificar se o banco de dados está acessível.

### Passo 2: Restauração dos Dados
1. Importar o arquivo de backup mais recente:
   ```bash
  cat backup_xxx.sql | docker exec -i lex-db psql -U admin lexcrm_db
   ```

### Passo 3: Validação
1. Realizar login com usuário Admin.
2. Verificar integridade dos últimos Clientes e Pedidos cadastrados.

## RPO e RTO
- **RPO (Ponto de Recuperação):** Máximo de 24 horas de perda de dados.
- **RTO (Tempo de Recuperação):** Sistema deve estar online em até 2 horas após o incidente.
