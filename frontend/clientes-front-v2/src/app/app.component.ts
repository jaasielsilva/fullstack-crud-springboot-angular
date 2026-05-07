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
        // Oculta a sidebar tanto na tela de login quanto na de recuperação de senha
        const url = event.urlAfterRedirects;
        this.isLoginPage = url.includes('/login') || url.includes('/forgot-password');
      }
    });
  }

  sair(): void {
    this.authService.logout();
  }
}
