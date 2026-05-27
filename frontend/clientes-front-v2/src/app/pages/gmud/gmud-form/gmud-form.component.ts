import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { GmudService } from '../../../services/gmud.service';
import {
  ChangeType,
  CreateChangeRequest,
  DeployEnvironment,
  RiskLevel
} from '../../../models/gmud/change-request.model';

@Component({
  selector: 'app-gmud-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './gmud-form.component.html',
  styleUrl: './gmud-form.component.css'
})
export class GmudFormComponent {
  form: CreateChangeRequest = {
    title: '',
    description: '',
    type: 'NORMAL',
    environment: 'HML',
    riskLevel: 'MEDIUM',
    impactDescription: '',
    rollbackPlan: ''
  };

  readonly types: ChangeType[] = ['NORMAL', 'EMERGENCY', 'STANDARD'];
  readonly envs: DeployEnvironment[] = ['DEV', 'HML', 'PROD'];
  readonly risks: RiskLevel[] = ['LOW', 'MEDIUM', 'HIGH'];

  salvando = false;
  erro = '';

  constructor(private gmudService: GmudService, private router: Router) {}

  salvar(): void {
    if (!this.form.title?.trim()) {
      this.erro = 'Título é obrigatório.';
      return;
    }
    if (!this.form.rollbackPlan?.trim()) {
      this.erro = 'Plano de rollback é obrigatório.';
      return;
    }
    this.salvando = true;
    this.erro = '';
    this.gmudService.criar(this.form).subscribe({
      next: (created) => {
        this.router.navigate(['/gmud', created.id]);
      },
      error: (err) => {
        this.erro = err?.error?.erro || 'Não foi possível criar a GMUD.';
        this.salvando = false;
      }
    });
  }
}
