import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { environment } from '../../../environments/environment';
import {
  DEPLOY_FLOW_STEPS,
  FLOW_LOCATION_BADGE,
  FLOW_LOCATION_LABEL
} from '../../shared/deploy-flow/deploy-flow.steps';
import { currentDeployTier } from '../../shared/deploy-flow/deploy-flow.context';

@Component({
  selector: 'app-fluxo-mudanca',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './fluxo-mudanca.component.html',
  styleUrl: './fluxo-mudanca.component.css'
})
export class FluxoMudancaComponent {
  readonly steps = DEPLOY_FLOW_STEPS;
  readonly locationLabel = FLOW_LOCATION_LABEL;
  readonly locationBadge = FLOW_LOCATION_BADGE;
  readonly appTier = currentDeployTier();
  readonly hmlUrl = environment.hmlAppUrl;
  readonly prodUrl = environment.prodAppUrl;
  readonly apiHint =
    environment.deployTier === 'PROD'
      ? 'api.erpcorporativo.shop'
      : 'api.dev.erpcorporativo.shop';
}
