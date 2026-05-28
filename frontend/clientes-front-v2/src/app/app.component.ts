import { Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './security/auth.service';
import { TrialBannerComponent } from './shared/components/trial-banner/trial-banner.component';
import { TaskNotificationsBellComponent } from './shared/components/task-notifications-bell/task-notifications-bell.component';
import { showTasksModuleInUi } from './shared/deploy-flow/platform-ops.context';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, CommonModule, TrialBannerComponent, TaskNotificationsBellComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  /** Telas sem sidebar (login, trial público, checkout/planos, retorno Mercado Pago). */
  minimalShell = false;

  /** Menu lateral visível (persistido em localStorage). */
  sidebarOpen = true;

  private readonly sidebarStorageKey = 'lexcrm.sidebar.open';

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

  ngOnInit(): void {
    const saved = localStorage.getItem(this.sidebarStorageKey);
    if (saved !== null) {
      this.sidebarOpen = saved === 'true';
      return;
    }
    this.sidebarOpen = window.matchMedia('(min-width: 992px)').matches;
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
    localStorage.setItem(this.sidebarStorageKey, String(this.sidebarOpen));
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

  /** Tarefas e sino: somente homologação (PROD usa GMUD). */
  showTasksInUi(): boolean {
    return this.canAccessSuperAdmin() && showTasksModuleInUi();
  }
}
