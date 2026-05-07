import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Cliente } from './cliente.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private apiUrl = `${environment.apiUrl}/api/clientes`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Cliente[]> {
    console.log('[ClienteService] GET', this.apiUrl);
    return this.http.get<Cliente[]>(this.apiUrl).pipe(
      tap(data => console.log(`[ClienteService] ${data.length} cliente(s) carregado(s)`, data))
    );
  }

  salvar(cliente: Cliente): Observable<Cliente> {
    console.log('[ClienteService] POST', cliente);
    return this.http.post<Cliente>(this.apiUrl, cliente).pipe(
      tap(novo => console.log('[ClienteService] Criado:', novo))
    );
  }

  atualizar(id: number, cliente: Cliente): Observable<Cliente> {
    console.log('[ClienteService] PUT id:', id, cliente);
    return this.http.put<Cliente>(`${this.apiUrl}/${id}`, cliente).pipe(
      tap(atualizado => console.log('[ClienteService] Atualizado:', atualizado))
    );
  }

  deletar(id: number): Observable<void> {
    console.log('[ClienteService] DELETE id:', id);
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => console.log('[ClienteService] Excluído id:', id))
    );
  }
}
