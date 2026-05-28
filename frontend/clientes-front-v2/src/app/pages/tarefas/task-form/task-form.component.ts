import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TaskService } from '../../../services/task.service';
import { CreateWorkTask } from '../../../models/task/work-task.model';

@Component({
  selector: 'app-task-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './task-form.component.html',
  styleUrl: './task-form.component.css'
})
export class TaskFormComponent {
  form: CreateWorkTask = { title: '', description: '' };
  salvando = false;
  erro = '';

  constructor(private taskService: TaskService, private router: Router) {}

  salvar(): void {
    if (!this.form.title?.trim()) {
      this.erro = 'Título é obrigatório.';
      return;
    }
    this.salvando = true;
    this.erro = '';
    this.taskService.criar(this.form).subscribe({
      next: (created) => this.router.navigate(['/tarefas', created.id]),
      error: (err) => {
        this.erro = err?.error?.erro || 'Não foi possível criar a tarefa.';
        this.salvando = false;
      }
    });
  }
}
