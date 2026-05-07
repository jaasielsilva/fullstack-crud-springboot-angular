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
}
