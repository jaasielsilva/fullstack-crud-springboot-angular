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

  private decodeJwtPayload(token: string): any | null {
    try {
      const payloadBase64Url = token.split('.')[1];
      if (!payloadBase64Url) return null;
      // JWT usa base64url; convertemos para base64 tradicional antes do atob.
      const payloadBase64 = payloadBase64Url.replace(/-/g, '+').replace(/_/g, '/');
      const padded = payloadBase64 + '='.repeat((4 - (payloadBase64.length % 4)) % 4);
      return JSON.parse(atob(padded));
    } catch (e) {
      console.error('Erro ao decodificar token', e);
      return null;
    }
  }

  private normalizeRole(role: string | null | undefined): string {
    if (!role) return '';
    return role.toUpperCase().replace(/^ROLE_/, '').trim();
  }

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
    const payloadJson = this.decodeJwtPayload(token);
    return payloadJson?.role ?? null;
  }

  // Verifica se o usuário tem alguma das roles permitidas
  hasRole(allowedRoles: string[]): boolean {
    const userRole = this.normalizeRole(this.getRole());
    if (!userRole) return false;
    const normalizedAllowedRoles = allowedRoles.map(r => this.normalizeRole(r));
    return normalizedAllowedRoles.includes(userRole);
  }

  getUserInfo(): any {
    const token = this.getToken();
    if (!token) return { sub: 'Visitante', role: 'USER' };
    return this.decodeJwtPayload(token) ?? { sub: 'Erro', role: 'USER' };
  }

  // Super Admin é o administrador da Matriz (Tenant ID: 1)
  isSuperAdmin(): boolean {
    const info = this.getUserInfo();
    return this.normalizeRole(info?.role) === 'ADMIN' && info?.tenantId === 1;
  }
}
