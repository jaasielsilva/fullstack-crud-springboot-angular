# Homologação (HML): derrubar stack, apagar banco (volume) e subir de novo

Use na VPS em **`/opt/erpcorporativo/hml`** (ou o diretório onde está o `docker-compose.yml` de HML).

**Atenção:** isso remove **containers**, **rede do projeto** e o **volume do MySQL** (`mysql_hml_data`). Todo o **dado do banco de homologação** é apagado. Não execute em produção.

## 1. Parar tudo e remover volumes

```bash
cd /opt/erpcorporativo/hml
docker compose down -v --remove-orphans
```

- `down`: para e remove os serviços deste compose.
- `-v`: remove **volumes nomeados** declarados no arquivo (dados persistentes do MySQL).
- `--remove-orphans`: remove containers órfãos ligados a este projeto.

Saída esperada (exemplo): remoção de `web-hml`, `api-hml`, `mysql-hml`, rede `hml_internal-hml` e volume `hml_mysql_hml_data`.

## 2. (Opcional) Conferir se não sobrou volume HML

```bash
docker volume ls | grep -i hml
```

Se não imprimir nada, não há volumes com nome contendo `hml`.

## 3. Subir o stack de novo (banco vazio)

```bash
cd /opt/erpcorporativo/hml
docker compose up -d
```

Aguarde o MySQL ficar **healthy**; a API e o web sobem em seguida. O schema/tabelas dependem do **`JPA_DDL_AUTO`** no `.env` / compose: use **`update`** (padrão do compose) para HML com dados persistentes; **`create`** apaga e recria tabelas a cada subida da API — só faz sentido para reset rápido de desenvolvimento.

## Referência rápida (copiar tudo)

```bash
cd /opt/erpcorporativo/hml
docker compose down -v --remove-orphans
docker volume ls | grep -i hml
docker compose up -d
```

## Nota

- Isso **não** remove imagens Docker (`docker images`); só o que o `docker compose` gerencia nesta pasta.
- Produção (`/opt/erpcorporativo/prod` ou equivalente) usa outro compose — **não** misture os comandos.
