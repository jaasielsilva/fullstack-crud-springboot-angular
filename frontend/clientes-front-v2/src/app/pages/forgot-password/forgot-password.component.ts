import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Router, RouterModule } from '@angular/router';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule, RouterModule],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  
  email = '';
  token = '';
  novaSenha = '';
  
  etapa: 'SOLICITAR' | 'REDEFINIR' = 'SOLICITAR';
  carregando = false;
  mensagemSucesso = '';
  mensagemErro = '';

  private apiUrl = `${environment.apiUrl}/api/auth`;

  constructor(private http: HttpClient, private router: Router) {}

  solicitarCodigo() {
    if (!this.email) return;
    this.carregando = true;
    this.mensagemErro = '';

    this.http.post(`${this.apiUrl}/forgot-password`, { email: this.email }).subscribe({
      next: (res: any) => {
        this.mensagemSucesso = res.mensagem;
        this.etapa = 'REDEFINIR';
        this.carregando = false;
      },
      error: (err) => {
        this.mensagemErro = err.error?.erro || 'Erro ao solicitar código.';
        this.carregando = false;
      }
    });
  }

  redefinirSenha() {
    if (!this.token || !this.novaSenha) return;
    this.carregando = true;
    this.mensagemErro = '';

    this.http.post(`${this.apiUrl}/reset-password`, { 
      token: this.token, 
      novaSenha: this.novaSenha 
    }).subscribe({
      next: (res: any) => {
        this.mensagemSucesso = 'Senha redefinida com sucesso! Redirecionando...';
        this.carregando = false;
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        this.mensagemErro = err.error?.erro || 'Código inválido ou expirado.';
        this.carregando = false;
      }
    });
  }
}
