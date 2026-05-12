import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Pedido, PedidoRequest, CheckoutPedidoResponse } from './pedido.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PedidoService {
  private apiUrl = `${environment.apiUrl}/api/pedidos`;

  constructor(private http: HttpClient) {}

  listarPedidos(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(this.apiUrl);
  }

  criarPedido(payload: PedidoRequest): Observable<Pedido> {
    return this.http.post<Pedido>(this.apiUrl, payload);
  }

  atualizarPedido(id: number, payload: PedidoRequest): Observable<Pedido> {
    return this.http.put<Pedido>(`${this.apiUrl}/${id}`, payload);
  }

  buscarPorId(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.apiUrl}/${id}`);
  }

  deletarPedido(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  checkoutPedido(id: number): Observable<CheckoutPedidoResponse> {
    return this.http.post<CheckoutPedidoResponse>(`${this.apiUrl}/${id}/checkout`, {});
  }

  marcarEntregue(id: number): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.apiUrl}/${id}/marcar-entregue`, {});
  }

  cancelarPedido(id: number): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.apiUrl}/${id}/cancelar`, {});
  }

  simularPagamento(id: number): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.apiUrl}/${id}/simular-pagamento`, {});
  }
}