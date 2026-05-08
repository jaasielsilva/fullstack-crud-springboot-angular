import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Pedido, PedidoRequest } from './pedido.model';

@Injectable({
  providedIn: 'root'
})
export class PedidoService {

  private apiUrl = 'http://localhost:8080/api/pedidos';

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
}