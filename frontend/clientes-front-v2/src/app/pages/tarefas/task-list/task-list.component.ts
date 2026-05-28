import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TaskService } from '../../../services/task.service';
import { TaskStatus, WorkTask } from '../../../models/task/work-task.model';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './task-list.component.html',
  styleUrl: './task-list.component.css'
})
export class TaskListComponent implements OnInit {
  tasks: WorkTask[] = [];
  carregando = false;
  filtroStatus: TaskStatus | '' = '';
  page = 0;
  readonly pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  readonly statusOptions: TaskStatus[] = ['OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED'];

  constructor(private taskService: TaskService) {}

  ngOnInit(): void {
    this.carregar();
  }

  carregar(resetPage = false): void {
    if (resetPage) this.page = 0;
    this.carregando = true;
    this.taskService.listar(this.filtroStatus || undefined, this.page, this.pageSize).subscribe({
      next: (data) => {
        this.tasks = data.content;
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
