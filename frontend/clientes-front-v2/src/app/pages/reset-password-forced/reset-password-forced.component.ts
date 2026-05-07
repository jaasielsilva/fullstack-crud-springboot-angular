import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../../security/auth.service';
import { ConfirmModalComponent } from '../../shared/components/confirm-modal/confirm-modal.component';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-reset-password-forced',
  standalone: true,
  imports: [CommonModule, FormsModule, ConfirmModalComponent],
  templateUrl: './reset-password-forced.component.html',
  styleUrl: './reset-password-forced.component.css'
})
export class ResetPasswordForcedComponent {
  
  senha = '';
  confirmarSenha = '';
  error = '';
  loading = false;
  showSuccessModal = false;

  constructor(
    private http: HttpClient, 
    private router: Router, 
    private authService: AuthService
  ) {}

  confirmar() {
    if (this.senha !== this.confirmarSenha) {
      this.error = 'As senhas não coincidem!';
      return;
    }

    if (this.senha.length < 6) {
      this.error = 'A senha deve ter pelo menos 6 caracteres!';
      return;
    }

    this.loading = true;
    this.http.post(`${environment.apiUrl}/api/auth/reset-password-forced`, { novaSenha: this.senha })
      .subscribe({
        next: () => {
          this.loading = false;
          this.showSuccessModal = true;
        },
        error: (err) => {
          this.error = 'Erro ao alterar senha. Tente novamente.';
          this.loading = false;
        }
      });
  }

  handleModalConfirm() {
    this.authService.logout();
  }
}
