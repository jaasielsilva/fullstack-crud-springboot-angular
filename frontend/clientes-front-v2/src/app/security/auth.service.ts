import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { SubscriptionSnapshot } from '../models/subscription.model';

/** Resposta do login e do cadastro trial (backend unificado). */
interface AuthPayloadResponse {
  token: string;
  subscription?: SubscriptionSnapshot;
}

const SUBSCRIPTION_STORAGE_KEY = 'subscriptionContext';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/api/auth`;
  private readonly loggedIn = new BehaviorSubject<boolean>(this.hasToken());

  constructor(private http: HttpClient, private router: Router) {}

  isLoggedIn$(): Observable<boolean> {
    return this.loggedIn.asObservable();
  }

  private hasToken(): boolean {
    return !!localStorage.getItem('token');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  login(dados: { login: string; senha: string }): Observable<AuthPayloadResponse> {
    return this.http.post<AuthPayloadResponse>(`${this.apiUrl}/login`, dados).pipe(
      tap((response) => this.persistSession(response))
    );
  }

  cadastroTrial(dados: {
    nomeEmpresa: string;
    nomeResponsavel: string;
    email: string;
    telefone: string;
    senha: string;
  }): Observable<AuthPayloadResponse> {
    return this.http
      .post<AuthPayloadResponse>(`${environment.apiUrl}/api/public/cadastro-trial`, dados)
      .pipe(tap((response) => this.persistSession(response)));
  }

  private persistSession(response: AuthPayloadResponse): void {
    localStorage.setItem('token', response.token);
    if (response.subscription) {
      localStorage.setItem(SUBSCRIPTION_STORAGE_KEY, JSON.stringify(response.subscription));
    }
    this.loggedIn.next(true);
  }

  refreshSubscriptionContext(): Observable<SubscriptionSnapshot> {
    return this.http.get<SubscriptionSnapshot>(`${this.apiUrl}/me`).pipe(
      tap((snapshot) =>
        localStorage.setItem(SUBSCRIPTION_STORAGE_KEY, JSON.stringify(snapshot))
      )
    );
  }

  getSubscriptionContext(): SubscriptionSnapshot | null {
    const raw = localStorage.getItem(SUBSCRIPTION_STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as SubscriptionSnapshot;
    } catch {
      return null;
    }
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem(SUBSCRIPTION_STORAGE_KEY);
    this.loggedIn.next(false);
    this.router.navigate(['/login']);
  }

  getRole(): string | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      const payloadBase64 = token.split('.')[1];
      const payloadJson = JSON.parse(atob(payloadBase64));
      return payloadJson.role || null;
    } catch (e) {
      console.error('Erro ao decodificar token', e);
      return null;
    }
  }

  hasRole(allowedRoles: string[]): boolean {
    const userRole = this.getRole();
    if (!userRole) return false;
    return allowedRoles.includes(userRole);
  }

  getUserInfo(): {
    sub?: string;
    role?: string;
    tenantId?: number;
    mustChangePassword?: boolean;
  } {
    const token = this.getToken();
    if (!token) return { sub: 'Visitante', role: 'USER' };
    try {
      const payloadBase64 = token.split('.')[1];
      return JSON.parse(atob(payloadBase64));
    } catch (e) {
      return { sub: 'Erro', role: 'USER' };
    }
  }

  getTenantId(): number | null {
    const tid = this.getUserInfo().tenantId;
    return tid !== undefined && tid !== null ? Number(tid) : null;
  }

  isSuperAdmin(): boolean {
    const info = this.getUserInfo();
    return info.role === 'ADMIN' && info.tenantId === 1;
  }
}
