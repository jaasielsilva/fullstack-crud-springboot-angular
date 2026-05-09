import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './security/auth.service';
import { TrialBannerComponent } from './shared/components/trial-banner/trial-banner.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, CommonModule, TrialBannerComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  /** Telas sem sidebar (login, trial público, checkout/planos, retorno Mercado Pago). */
  minimalShell = false;

  constructor(private router: Router, private authService: AuthService) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        const url = event.urlAfterRedirects.split('?')[0];
        this.minimalShell =
          url.startsWith('/login') ||
          url.startsWith('/forgot-password') ||
          url.startsWith('/cadastro-trial') ||
          url.startsWith('/planos') ||
          url.startsWith('/pagamento') ||
          url.startsWith('/trial-expirado');
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
