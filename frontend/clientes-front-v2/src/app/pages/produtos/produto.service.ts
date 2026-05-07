import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Produto } from './produto.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProdutoService {
  private apiUrl = `${environment.apiUrl}/api/produtos`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Produto[]> {
    return this.http.get<Produto[]>(this.apiUrl).pipe(
      tap(data => console.log(`[ProdutoService] ${data.length} produto(s) carregado(s)`))
    );
  }

  salvar(produto: Produto): Observable<Produto> {
    return this.http.post<Produto>(this.apiUrl, produto).pipe(
      tap(novo => console.log('[ProdutoService] Criado:', novo))
    );
  }

  atualizar(id: number, produto: Produto): Observable<Produto> {
    return this.http.put<Produto>(`${this.apiUrl}/${id}`, produto).pipe(
      tap(atualizado => console.log('[ProdutoService] Atualizado:', atualizado))
    );
  }

  deletar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => console.log('[ProdutoService] Excluído id:', id))
    );
  }
}
