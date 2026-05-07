import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../security/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  loginData = {
    login: '',
    senha: ''
  };

  carregando = false;
  mensagemErro = '';

  constructor(private authService: AuthService, private router: Router) {}

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
        // Se der sucesso, vai direto para o Dashboard
        this.router.navigate(['/dashboard']);
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
}
