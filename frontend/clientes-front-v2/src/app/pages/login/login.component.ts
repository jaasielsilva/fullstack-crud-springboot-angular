import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../security/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
  loginData = {
    login: '',
    senha: ''
  };

  carregando = false;
  mensagemErro = '';
  /** iPhone abrindo no Chrome (Telegram/navegador padrão) — PWA exige Safari. */
  mostrarAvisoSafari = false;
  urlAtual = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const ua = navigator.userAgent;
    const ios = /iPhone|iPad|iPod/i.test(ua);
    const chromeIos = /CriOS/i.test(ua);
    const standalone =
      ('standalone' in navigator &&
        (navigator as Navigator & { standalone?: boolean }).standalone) ||
      window.matchMedia('(display-mode: standalone)').matches;
    this.mostrarAvisoSafari = ios && chromeIos && !standalone;
    this.urlAtual = window.location.href;
  }

  entrar(): void {
    if (!this.loginData.login || !this.loginData.senha) {
      this.mensagemErro = 'Preencha email e senha.';
      return;
    }

    this.carregando = true;
    this.mensagemErro = '';

    this.authService.login(this.loginData).subscribe({
      next: () => {
        this.carregando = false;
        const allowed = this.authService.hasRole(['ADMIN', 'GERENTE', 'VENDEDOR', 'SUPORTE']);
        const userInfo = this.authService.getUserInfo();
        if (userInfo?.mustChangePassword) {
          this.router.navigate(['/reset-password-forced']);
        } else if (allowed) {
          this.redirectAfterLogin();
        } else {
          this.router.navigate(['/sem-permissao']);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.carregando = false;
        // Pega a mensagem de erro que mandamos do Spring Boot (Ex: "Email ou senha incorretos")
        if (err.error && err.error.erro) {
          this.mensagemErro = err.error.erro;
        } else {
          this.mensagemErro = 'Servidor indisponível no momento.';
        }
      }
    });
  }

  /** Volta para deep link (ex.: /gmud/42 do Telegram) após login. */
  private redirectAfterLogin(): void {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    const safe =
      returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//')
        ? returnUrl
        : null;
    this.router.navigateByUrl(safe ?? '/dashboard');
  }
}
