import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/api/auth`;
  private loggedIn = new BehaviorSubject<boolean>(this.hasToken());

  constructor(private http: HttpClient, private router: Router) {}

  // Observable para a interface reagir a mudanças de login/logout
  isLoggedIn$(): Observable<boolean> {
    return this.loggedIn.asObservable();
  }

  // Verifica se o token existe no LocalStorage
  private hasToken(): boolean {
    return !!localStorage.getItem('token');
  }

  // Retorna o token para o Interceptor usar
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  login(dados: any): Observable<any> {
    return this.http.post<{token: string}>(`${this.apiUrl}/login`, dados).pipe(
      tap(response => {
        // Quando o login dá certo, salvamos o token e avisamos o sistema
        localStorage.setItem('token', response.token);
        this.loggedIn.next(true);
      })
    );
  }

  // Reemite o token a partir da role/dados atuais do usuário no banco.
  // Útil quando o ADMIN libera permissão e o usuário quer aproveitar
  // o novo nível de acesso sem precisar deslogar e logar de novo.
  refreshToken(): Observable<any> {
    return this.http.post<{token: string}>(`${this.apiUrl}/refresh`, {}).pipe(
      tap(response => {
        localStorage.setItem('token', response.token);
        this.loggedIn.next(true);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    this.loggedIn.next(false);
    this.router.navigate(['/login']);
  }

  // Decodifica o payload do JWT para pegar a Role
  getRole(): string | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      // O JWT é composto por Header.Payload.Signature. O payload é a parte 1.
      const payloadBase64 = token.split('.')[1];
      const payloadJson = JSON.parse(atob(payloadBase64));
      return payloadJson.role || null;
    } catch (e) {
      console.error('Erro ao decodificar token', e);
      return null;
    }
  }

  // Verifica se o usuário tem alguma das roles permitidas
  hasRole(allowedRoles: string[]): boolean {
    const userRole = this.getRole();
    if (!userRole) return false;
    return allowedRoles.includes(userRole);
  }

  getUserInfo(): any {
    const token = this.getToken();
    if (!token) return { sub: 'Visitante', role: 'USER' };
    try {
      const payloadBase64 = token.split('.')[1];
      return JSON.parse(atob(payloadBase64));
    } catch (e) {
      return { sub: 'Erro', role: 'USER' };
    }
  }

  // Super Admin é o administrador da Matriz (Tenant ID: 1)
  isSuperAdmin(): boolean {
    const info = this.getUserInfo();
    return info.role === 'ADMIN' && info.tenantId === 1;
  }
}
