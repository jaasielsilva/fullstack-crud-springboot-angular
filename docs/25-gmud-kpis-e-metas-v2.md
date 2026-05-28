# Módulo 25: KPIs e Metas de Maturidade GMUD (v2.0)

## Objetivo
Medir a qualidade do processo de mudanças e suportar melhoria contínua baseada em dados.

## KPIs principais

| KPI | Definição | Meta sugerida | Fonte |
|---|---|---|---|
| Lead time mudança | Solicitação até produção | Redução contínua | GMUD + GitHub |
| Sucesso deploy PROD | Deploys concluídos sem incidente crítico | >= 98% | Workflows |
| Rollback rate | Mudanças com rollback / total | <= 2% | GMUD |
| Mudanças emergenciais | Emergenciais / total | Tendência de queda | GMUD |
| Tempo de aprovação GMUD | Abertura até `APPROVED` | Meta por criticidade | GMUD |
| Conformidade de evidências | Mudanças com evidência completa | 100% | GMUD + release |

## Periodicidade de revisão
- Semanal: operação e incidentes.
- Mensal: tendência de KPIs e ações corretivas.
- Trimestral: revisão de política (janela, freeze, emergência).

## Painel mínimo de acompanhamento
- Mudanças por status
- Mudanças por tipo (NORMAL/EMERGENCY)
- Taxa de sucesso de deploy por ambiente
- Rollbacks por período
- Tempo médio por fase

## Plano de ação quando meta não é atingida
1. Identificar gargalo (aprovação, testes, deploy, rollback).
2. Definir ação corretiva com dono e prazo.
3. Reavaliar KPI no ciclo seguinte.
4. Registrar lições aprendidas.

