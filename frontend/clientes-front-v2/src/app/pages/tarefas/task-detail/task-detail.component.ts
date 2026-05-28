import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TaskService } from '../../../services/task.service';
import { GmudService } from '../../../services/gmud.service';
import { TaskStatus, WorkTask } from '../../../models/task/work-task.model';
import { ChangeRequest } from '../../../models/gmud/change-request.model';
import { TaskFlowPanelComponent } from '../../../shared/deploy-flow/task-flow-panel.component';

@Component({
  selector: 'app-task-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, TaskFlowPanelComponent],
  templateUrl: './task-detail.component.html',
  styleUrl: './task-detail.component.css'
})
export class TaskDetailComponent implements OnInit {
  task: WorkTask | null = null;
  gmuds: ChangeRequest[] = [];
  carregando = true;
  acaoEmAndamento = false;
  erro = '';
  sucesso = '';
  branchCopiada = false;

  constructor(
    private route: ActivatedRoute,
    private taskService: TaskService,
    private gmudService: GmudService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.carregando = false;
      return;
    }
    this.carregar(id);
  }

  carregar(id: number): void {
    this.carregando = true;
    this.taskService.buscar(id).subscribe({
      next: (task) => {
        this.task = task;
        this.carregarGmud(id);
      },
      error: () => {
        this.erro = 'Tarefa não encontrada.';
        this.carregando = false;
      }
    });
  }

  private carregarGmud(taskId: number): void {
    this.gmudService.listar(undefined, undefined, taskId, 0, 20).subscribe({
      next: (page) => {
        this.gmuds = page.content;
        this.carregando = false;
      },
      error: () => {
        this.carregando = false;
      }
    });
  }

  copiarBranch(): void {
    if (!this.task?.branchName) return;
    navigator.clipboard.writeText(this.task.branchName).then(() => {
      this.branchCopiada = true;
      setTimeout(() => (this.branchCopiada = false), 2000);
    });
  }

  iniciar(): void {
    if (!this.task) return;
    this.executar(() => this.taskService.iniciar(this.task!.id));
  }

  concluir(): void {
    if (!this.task) return;
    this.executar(() => this.taskService.concluir(this.task!.id));
  }

  cancelar(): void {
    if (!this.task) return;
    this.executar(() => this.taskService.cancelar(this.task!.id));
  }

  private executar(fn: () => ReturnType<TaskService['iniciar']>): void {
    this.acaoEmAndamento = true;
    this.erro = '';
    this.sucesso = '';
    fn().subscribe({
      next: (updated) => {
        this.task = updated;
        this.sucesso = 'Ação realizada com sucesso.';
        this.acaoEmAndamento = false;
        this.carregarGmud(updated.id);
      },
      error: (err) => {
        this.erro = err?.error?.erro || 'Falha na operação.';
        this.acaoEmAndamento = false;
      }
    });
  }

  badgeClass(status: TaskStatus): string {
    const map: Record<TaskStatus, string> = {
      OPEN: 'bg-secondary',
      IN_PROGRESS: 'bg-primary',
      DONE: 'bg-success',
      CANCELLED: 'bg-danger'
    };
    return map[status] ?? 'bg-secondary';
  }

  labelStatus(status: TaskStatus): string {
    return status.replace('_', ' ');
  }
}
