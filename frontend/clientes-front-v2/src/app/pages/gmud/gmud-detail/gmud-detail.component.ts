import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { GmudService } from '../../../services/gmud.service';
import {
  ChangeRequest,
  ChangeStatus
} from '../../../models/gmud/change-request.model';

@Component({
  selector: 'app-gmud-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './gmud-detail.component.html',
  styleUrl: './gmud-detail.component.css'
})
export class GmudDetailComponent implements OnInit {
  change: ChangeRequest | null = null;
  carregando = false;
  acaoEmAndamento = false;
  comentario = '';
  comentarioRollback = '';
  erro = '';
  sucesso = '';

  constructor(
    private route: ActivatedRoute,
    private gmudService: GmudService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('id'));
      if (id) this.carregar(id);
    });
  }

  carregar(id: number): void {
    this.carregando = true;
    this.gmudService.buscar(id).subscribe({
      next: (data) => {
        this.change = data;
        this.carregando = false;
      },
      error: () => {
        this.erro = 'GMUD não encontrada.';
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

  submeter(): void {
    if (!this.change) return;
    this.executar(() => this.gmudService.submeter(this.change!.id, this.comentario || undefined));
  }

  aprovar(): void {
    if (!this.change) return;
    this.executar(() => this.gmudService.aprovar(this.change!.id, this.comentario || undefined));
  }

  implantar(): void {
    if (!this.change) return;
    this.executar(() => this.gmudService.implantar(this.change!.id, this.comentario || undefined));
  }

  rollback(): void {
    if (!this.change || !this.comentarioRollback.trim()) {
      this.erro = 'Informe o motivo do rollback.';
      return;
    }
    this.executar(() => this.gmudService.rollback(this.change!.id, this.comentarioRollback));
  }

  private executar(fn: () => ReturnType<GmudService['submeter']>): void {
    this.acaoEmAndamento = true;
    this.erro = '';
    this.sucesso = '';
    fn().subscribe({
      next: (updated) => {
        this.change = updated;
        this.sucesso = 'Ação realizada com sucesso.';
        this.acaoEmAndamento = false;
        this.comentario = '';
      },
      error: (err) => {
        this.erro = err?.error?.erro || 'Falha na operação.';
        this.acaoEmAndamento = false;
      }
    });
  }
}
