import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
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

  logout(): void {
    localStorage.removeItem('token');
    this.loggedIn.next(false);
    this.router.navigate(['/login']);
  }
}
