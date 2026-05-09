import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../security/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-cadastro-trial',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cadastro-trial.component.html',
  styleUrl: './cadastro-trial.component.css'
})
export class CadastroTrialComponent {
  form = {
    nomeEmpresa: '',
    nomeResponsavel: '',
    email: '',
    telefone: '',
    senha: ''
  };

  carregando = false;
  erro = '';

  constructor(private auth: AuthService, private router: Router) {}

  enviar(): void {
    if (!this.form.nomeEmpresa || !this.form.nomeResponsavel || !this.form.email || !this.form.telefone || !this.form.senha) {
      this.erro = 'Preencha todos os campos.';
      return;
    }
    if (this.form.senha.length < 6) {
      this.erro = 'A senha deve ter pelo menos 6 caracteres.';
      return;
    }

    this.carregando = true;
    this.erro = '';

    this.auth.cadastroTrial(this.form).subscribe({
      next: () => {
        this.carregando = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err: HttpErrorResponse) => {
        this.carregando = false;
        if (err.error?.erro) {
          this.erro = err.error.erro;
        } else if (err.error?.campos) {
          this.erro = Object.values(err.error.campos).join(' ');
        } else {
          this.erro = 'Não foi possível concluir o cadastro.';
        }
      }
    });
  }
}
