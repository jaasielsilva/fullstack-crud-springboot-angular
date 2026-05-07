import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../security/auth.service';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './access-denied.component.html',
  styleUrl: './access-denied.component.css'
})
export class AccessDeniedComponent {
  userRole: string | null;
  userLogin: string;
  verificando = false;
  mensagem = '';
  tipoMensagem: 'erro' | 'info' = 'info';

  constructor(private authService: AuthService, private router: Router) {
    this.userRole = this.authService.getRole();
    const info = this.authService.getUserInfo();
    this.userLogin = info?.sub ?? 'Visitante';
  }

  voltar(): void {
    this.router.navigate(['/dashboard']);
  }

  sair(): void {
    this.authService.logout();
  }

  // Pega um token novo do backend com a role atual do usuário no banco.
  // Se o admin já tiver liberado, redireciona pro dashboard automaticamente.
  recarregar(): void {
    this.verificando = true;
    this.mensagem = '';

    this.authService.refreshToken().subscribe({
      next: () => {
        const novaRole = this.authService.getRole();
        this.userRole = novaRole;
        this.verificando = false;

        if (novaRole && novaRole !== 'USER') {
          this.tipoMensagem = 'info';
          this.mensagem = `Permissão liberada como ${novaRole}! Redirecionando...`;
          setTimeout(() => this.router.navigate(['/dashboard']), 1200);
        } else {
          this.tipoMensagem = 'erro';
          this.mensagem = 'Ainda não há permissão liberada. Fale com o administrador.';
        }
      },
      error: () => {
        this.verificando = false;
        this.tipoMensagem = 'erro';
        this.mensagem = 'Não foi possível verificar agora. Tente fazer login novamente.';
      }
    });
  }
}
