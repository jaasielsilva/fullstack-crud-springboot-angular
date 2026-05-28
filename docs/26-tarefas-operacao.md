# Tarefas — planejamento de desenvolvimento

Módulo de **Tarefas** (super admin, `tenantId = 1`) para organizar o trabalho **antes** do deploy. A **GMUD** continua sendo o registro formal de mudança/deploy (ITIL).

**Guia visual na aplicação:** menu **Fluxo de mudança** (`/fluxo-mudanca`) e painel no detalhe de cada tarefa.

## Fluxo recomendado

1. **Nova tarefa** (`/tarefas/nova`) → status `OPEN`, branch sugerida `feature/TASK-{id}-{slug}`.
2. **Iniciar** → `IN_PROGRESS` → criar branch e desenvolver em `dev`.
3. Validar em **HML** (push `dev`).
4. **Rascunho GMUD (HML)** (botão na tarefa) → planejamento com `taskId` no banco de HML (não é a GMUD que o pipeline de PROD aprova).
5. PR `dev` → `main` → pipeline PROD (GMUD automática + aprovação).
6. **Concluir tarefa** → `DONE`.

## API

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/tasks?page=0&size=10&status=` | Lista paginada |
| GET | `/api/tasks/{id}` | Detalhe |
| POST | `/api/tasks` | Criar |
| POST | `/api/tasks/{id}/start` | Iniciar |
| POST | `/api/tasks/{id}/complete` | Concluir |
| POST | `/api/tasks/{id}/cancel` | Cancelar |
| POST | `/api/tasks/{id}/link-gmud` | Vincular GMUD existente |

GMUD com vínculo: `GET /api/gmud/changes?taskId={id}`.

## SQL em produção

Antes do deploy com `JPA_DDL_AUTO=validate`, executar:

`deploy/sql/V2__work_tasks.sql`
