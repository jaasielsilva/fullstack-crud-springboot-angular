# Módulo 4: Gestão de Estoque

## Objetivo
Rastrear entradas e saídas de produtos com precisão contábil.

## Backend (Spring Boot)
1. Criar entidade `MovimentacaoEstoque` (Produto_ID, Tipo [ENTRADA/SAIDA], Quantidade, Data, Motivo).
2. Impedir exclusão de produtos que possuam movimentação (usar Inativação).
3. Trigger/Lógica para impedir vendas (Saída) se a quantidade for maior que o saldo.

## Frontend (Angular 17)
1. Na tela de detalhes do Produto, criar uma aba "Histórico de Movimentações".
2. Criar Alertas visuais (Toast ou Badge vermelho) na lista de Produtos para itens com "Estoque Baixo" (ex: < 5 unidades).
