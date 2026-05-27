import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { GmudService } from '../../../services/gmud.service';
import {
  ChangeRequest,
  ChangeStatus,
  DeployEnvironment
} from '../../../models/gmud/change-request.model';

@Component({
  selector: 'app-gmud-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './gmud-list.component.html',
  styleUrl: './gmud-list.component.css'
})
export class GmudListComponent implements OnInit {
  changes: ChangeRequest[] = [];
  carregando = false;
  filtroStatus: ChangeStatus | '' = '';
  filtroEnv: DeployEnvironment | '' = '';

  readonly statusOptions: ChangeStatus[] = ['OPEN', 'IN_APPROVAL', 'APPROVED', 'DEPLOYED', 'ROLLBACK'];
  readonly envOptions: DeployEnvironment[] = ['DEV', 'HML', 'PROD'];

  constructor(private gmudService: GmudService) {}

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.gmudService
      .listar(
        this.filtroStatus || undefined,
        this.filtroEnv || undefined
      )
      .subscribe({
        next: (data) => {
          this.changes = data;
          this.carregando = false;
        },
        error: () => {
          this.carregando = false;
        }
      });
  }

  badgeClass(status: ChangeStatus): string {
    const map: Record<ChangeStatus, string> = {
      OPEN: 'bg-secondary',
      IN_APPROVAL: 'bg-warning text-dark',
      APPROVED: 'bg-info text-dark',
      DEPLOYED: 'bg-success',
      ROLLBACK: 'bg-danger'
    };
    return map[status] ?? 'bg-secondary';
  }

  labelStatus(status: ChangeStatus): string {
    return status.replace('_', ' ');
  }
}
