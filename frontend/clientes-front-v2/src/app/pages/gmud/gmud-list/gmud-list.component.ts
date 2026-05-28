import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { currentDeployTier } from '../../../shared/deploy-flow/deploy-flow.context';
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
  page = 0;
  readonly pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  readonly appTier = currentDeployTier();
  readonly hmlAppUrl = environment.hmlAppUrl;
  readonly prodAppUrl = environment.prodAppUrl;

  readonly statusOptions: ChangeStatus[] = ['OPEN', 'IN_APPROVAL', 'APPROVED', 'DEPLOYED', 'ROLLBACK'];
  readonly envOptions: DeployEnvironment[] = ['DEV', 'HML', 'PROD'];

  constructor(private gmudService: GmudService) {}

  ngOnInit(): void {
    this.carregar();
  }

  carregar(resetPage = false): void {
    if (resetPage) {
      this.page = 0;
    }
    this.carregando = true;
    this.gmudService
      .listar(
        this.filtroStatus || undefined,
        this.filtroEnv || undefined,
        this.page,
        this.pageSize
      )
      .subscribe({
        next: (data) => {
          this.changes = data.content;
          this.totalElements = data.totalElements;
          this.totalPages = data.totalPages;
          this.page = data.number;
          this.carregando = false;
        },
        error: () => {
          this.carregando = false;
        }
      });
  }

  onFiltroChange(): void {
    this.carregar(true);
  }

  paginaAnterior(): void {
    if (this.page > 0) {
      this.page--;
      this.carregar();
    }
  }

  paginaProxima(): void {
    if (this.page < this.totalPages - 1) {
      this.page++;
      this.carregar();
    }
  }

  get paginaExibicao(): number {
    return this.totalPages === 0 ? 0 : this.page + 1;
  }

  get temPaginaAnterior(): boolean {
    return this.page > 0;
  }

  get temPaginaProxima(): boolean {
    return this.totalPages > 0 && this.page < this.totalPages - 1;
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
