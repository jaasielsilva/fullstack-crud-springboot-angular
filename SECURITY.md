# Política de Segurança

## Versões suportadas

| Componente | Branch   | Suporte        |
|------------|----------|----------------|
| API (Java) | `main`   | Produção       |
| API (Java) | `dev`    | Homologação    |
| Web (Angular) | `main` | Produção    |
| Web (Angular) | `dev`  | Homologação |

Correções de segurança são aplicadas primeiro em `dev` (HML) e promovidas para `main` (PROD) após validação.

## Reportar uma vulnerabilidade

**Não abra issues públicas** para falhas de segurança.

Use o fluxo privado do GitHub:

1. Acesse o repositório → aba **Security**
2. Clique em **Report a vulnerability** (Relatar uma vulnerabilidade)
3. Descreva o problema com o máximo de detalhes possível (passos para reproduzir, impacto, componente afetado)

Alternativa: em **Security → Advisories → Report a vulnerability**.

## O que esperar

| Etapa              | Prazo estimado |
|--------------------|----------------|
| Confirmação de recebimento | 3 dias úteis |
| Avaliação inicial  | 7 dias úteis   |
| Correção (crítica/alta) | prioridade máxima |
| Divulgação coordenada | após correção disponível |

## Escopo

Esta política cobre:

- API Spring Boot (`backend/clientes-api`)
- Frontend Angular (`frontend/clientes-front-v2`)
- Imagens Docker publicadas no GHCR
- Workflows GitHub Actions deste repositório

## Boas práticas para quem reporta

- Inclua versão/commit ou ambiente (HML/PROD) quando possível
- Não explore a falha além do necessário para demonstrá-la
- Não divulgue publicamente antes de uma correção coordenada

Agradecemos reportes responsáveis.
