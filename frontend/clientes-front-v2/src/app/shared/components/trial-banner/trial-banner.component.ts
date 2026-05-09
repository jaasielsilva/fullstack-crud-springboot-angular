import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../security/auth.service';

@Component({
  selector: 'app-trial-banner',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './trial-banner.component.html',
  styleUrl: './trial-banner.component.css'
})
export class TrialBannerComponent {
  constructor(public auth: AuthService) {}

  get snapshot() {
    return this.auth.getSubscriptionContext();
  }
}
