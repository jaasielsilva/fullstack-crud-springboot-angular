import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './security/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  isLoginPage = false;

  constructor(private router: Router, private authService: AuthService) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        // Oculta a sidebar na tela de login, recuperação e redefinição forçada
        const url = event.urlAfterRedirects;
        this.isLoginPage = url.includes('/login') || url.includes('/forgot-password') || url.includes('/reset-password-forced');
      }
    });
  }

  getUser(): any {
    return this.authService.getUserInfo();
  }

  sair(): void {
    this.authService.logout();
  }

  canAccess(roles: string[]): boolean {
    return this.authService.hasRole(roles);
  }

  canAccessSuperAdmin(): boolean {
    return this.authService.isSuperAdmin();
  }
}
