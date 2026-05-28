# Módulo 22: Governança Empresarial de Mudanças (GMUD v2.0)

## Objetivo
Definir a governança corporativa do processo de mudança em produção com critérios auditáveis, controle de risco e rastreabilidade ponta a ponta.

## Escopo
- Solicitação de mudança
- Classificação e priorização
- Aprovação formal
- Go/No-Go
- Deploy controlado
- Encerramento com evidências

## Papéis e matriz RACI

| Etapa | Solicitante | Dev | Aprovador | Executor Release | Operações/SRE |
|---|---|---|---|---|---|
| Abrir solicitação | R | C | I | I | I |
| Planejar mudança | C | R | I | C | C |
| Implementar/testar | I | R | I | I | C |
| Abrir GMUD | C | R | I | C | I |
| Aprovar GMUD | I | C | A/R | I | I |
| Go/No-Go final | I | C | A | R | C |
| Deploy PROD | I | C | I | A/R | C |
| Pós-implantação | I | R | I | A | C |
| Encerramento GMUD | I | R | A | C | I |

R = Responsible, A = Accountable, C = Consulted, I = Informed

## Estados oficiais da mudança
- Solicitada
- Planejada
- Em desenvolvimento
- Em validação HML
- GMUD aberta
- GMUD em aprovação
- GMUD aprovada
- Deploy em execução
- Implantada
- Encerrada
- Rollback

## Classes de mudança
- **NORMAL:** ciclo padrão e janela planejada.
- **EMERGENCY:** fluxo acelerado com justificativa obrigatória e revisão posterior.

## Políticas obrigatórias

### Segregação de funções
- Recomendado: aprovador diferente do executor de release.
- Exceção: times enxutos com justificativa registrada na GMUD.

### Janela e freeze
- Toda mudança NORMAL deve ocorrer em janela aprovada.
- Períodos de freeze bloqueiam NORMAL (só EMERGENCY).
- Mudança fora de janela: justificativa obrigatória.

### Emergência
- Deve conter impacto, mitigação, rollback e responsável.
- Aprovação simplificada, porém obrigatória.
- Post-mortem em até 24/48h.

## Critérios de conformidade em produção
- PR aprovado e merge em `main`
- Tag + GitHub Release para o commit
- GMUD em `APPROVED`
- Evidências mínimas anexadas

