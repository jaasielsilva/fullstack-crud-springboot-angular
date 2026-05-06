# Módulo 3: Pedidos (Vendas)

## Objetivo
Criar o "coração" do sistema comercial, permitindo vender produtos para os clientes cadastrados.

## Backend (Spring Boot)
1. Criar entidade `Pedido` (Data, Status, Valor Total, Cliente_ID).
2. Criar entidade `ItemPedido` (Pedido_ID, Produto_ID, Quantidade, Preço Unitário).
3. Lógica para calcular o valor total do pedido com base nos itens.
4. Atualizar o estoque automaticamente ao confirmar o pedido.

## Frontend (Angular 17)
1. Criar tela de Listagem de Pedidos com status em cores (Pendente, Pago, Cancelado).
2. Criar tela de "Novo Pedido" (estilo PDV):
   - Seleção de Cliente (Autocomplete ou Dropdown).
   - Adição dinâmica de Produtos (botão "Adicionar Item").
   - Resumo do carrinho com cálculo total ao vivo.
