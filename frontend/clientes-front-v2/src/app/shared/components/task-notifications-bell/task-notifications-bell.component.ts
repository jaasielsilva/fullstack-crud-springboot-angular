import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { interval, catchError, of } from 'rxjs';
import { TaskService } from '../../../services/task.service';
import { TaskStatus, WorkTask } from '../../../models/task/work-task.model';

@Component({
  selector: 'app-task-notifications-bell',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './task-notifications-bell.component.html',
  styleUrl: './task-notifications-bell.component.css'
})
export class TaskNotificationsBellComponent implements OnInit {
  private readonly taskService = inject(TaskService);
  private readonly destroyRef = inject(DestroyRef);

  pendingCount = 0;
  tasks: WorkTask[] = [];
  carregando = false;
  erro = '';

  private readonly refreshMs = 60_000;

  ngOnInit(): void {
    this.carregar();
    interval(this.refreshMs)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.carregar());
  }

  carregar(): void {
    this.carregando = true;
    this.erro = '';
    this.taskService
      .listarPendentesMinhas(10)
      .pipe(
        catchError(() => {
          this.erro = 'Não foi possível carregar as tarefas.';
          return of({ pendingCount: 0, tasks: [] });
        })
      )
      .subscribe((data) => {
        this.pendingCount = data.pendingCount;
        this.tasks = data.tasks;
        this.carregando = false;
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
    const map: Record<TaskStatus, string> = {
      OPEN: 'Aberta',
      IN_PROGRESS: 'Em andamento',
      DONE: 'Concluída',
      CANCELLED: 'Cancelada'
    };
    return map[status] ?? status;
  }
}
