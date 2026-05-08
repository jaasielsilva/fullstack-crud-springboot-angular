export interface Pedido {
  id?: number;
  clienteId?: number;
  cliente: string;
  status: string;
  valorTotal: number;
  dataPedido: string;
  itens?: ItemPedidoResponse[];
}

export interface ItemPedidoResponse {
  produtoId?: number;
  produto: string;
  quantidade: number;
  valorUnitario: number;
  subtotal: number;
}

export interface ItemPedidoRequest {
  produtoId: number;
  quantidade: number;
}

export interface PedidoRequest {
  clienteId: number;
  itens: ItemPedidoRequest[];
}