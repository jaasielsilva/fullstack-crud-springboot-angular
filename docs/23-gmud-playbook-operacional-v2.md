# Módulo 23: Playbook Operacional GMUD (v2.0)

## Objetivo
Padronizar a execução operacional diária de mudanças do início ao encerramento.

## Fluxo operacional
1. Solicitação do gestor.
2. Triagem técnica (tipo, risco, impacto, rollback).
3. Desenvolvimento e PR.
4. Validação em HML.
5. Abertura da GMUD de produção.
6. Aprovação formal.
7. Checklist Go/No-Go.
8. Release + deploy manual PROD.
9. Validação pós-implantação.
10. Encerramento com evidências.

## Procedimento passo a passo

### 1) Solicitação
- Registrar objetivo, benefício, urgência e janela pretendida.

### 2) Triagem
- Classificar NORMAL/EMERGENCY.
- Definir risco e impacto.
- Definir rollback.

### 3) Execução técnica
- Branch dedicada.
- Implementação + testes.
- PR com revisão.

### 4) Homologação
- Deploy em HML.
- Evidência funcional anexada.

### 5) GMUD PROD
- Criar GMUD com:
  - resumo executivo
  - impacto/risco
  - rollback
  - janela
  - links (PR, commit, pipeline, evidências HML)

### 6) Aprovação
- Aprovador registra decisão e comentário.

### 7) Go/No-Go
- Release publicada
- GMUD `APPROVED`
- Janela ativa
- Monitoramento ativo
- Contingência pronta

### 8) Deploy PROD
- Execução manual do workflow oficial.
- Confirmar artifact/tag correta.

### 9) Pós-deploy
- Smoke de negócio
- Health técnico (`/actuator/health`)
- Janela de observação

### 10) Encerramento
- Registrar resultado final
- Atualizar evidências
- Encerrar GMUD

## Regras de decisão Go/No-Go
- **Go:** todos os critérios cumpridos.
- **No-Go:** qualquer critério crítico pendente.
- **Rollback:** incidente crítico ou degradação relevante após deploy.

