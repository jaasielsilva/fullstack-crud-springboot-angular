import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ClienteService } from './cliente.service';
import { Cliente } from './cliente.model';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './clientes.component.html',
  styleUrl: './clientes.component.css'
})
export class ClientesComponent implements OnInit {
  clientes: Cliente[] = [];
  form: Cliente = this.formVazio();
  editandoId: number | null = null;
  mensagem = '';
  tipoMensagem: 'success' | 'danger' = 'success';
  carregando = false;

  constructor(private service: ClienteService) {}

  ngOnInit(): void {
    console.log('[Clientes] Iniciado');
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.service.listar().subscribe({
      next: (data) => {
        this.clientes = data;
        this.carregando = false;
        console.log('[Clientes] Lista:', data.length, 'item(s)');
      },
      error: (err) => {
        console.error('[Clientes] Erro ao carregar:', err);
        this.alerta('Não foi possível conectar ao servidor.', 'danger');
        this.carregando = false;
      }
    });
  }

  salvar(): void {
    if (!this.form.nome?.trim() || !this.form.email?.trim()) {
      this.alerta('Nome e email são obrigatórios.', 'danger');
      return;
    }

    if (this.editandoId !== null) {
      this.service.atualizar(this.editandoId, this.form).subscribe({
        next: (atualizado) => {
          this.clientes = this.clientes.map(c => c.id === atualizado.id ? atualizado : c);
          this.alerta('Cliente atualizado com sucesso!', 'success');
          this.resetar();
        },
        error: (err: HttpErrorResponse) => this.alerta(this.mensagemErro(err), 'danger')
      });
    } else {
      this.service.salvar(this.form).subscribe({
        next: (novo) => {
          this.clientes = [...this.clientes, novo];
          this.alerta('Cliente cadastrado com sucesso!', 'success');
          this.resetar();
        },
        error: (err: HttpErrorResponse) => this.alerta(this.mensagemErro(err), 'danger')
      });
    }
  }

  editar(cliente: Cliente): void {
    this.editandoId = cliente.id!;
    this.form = { ...cliente };
    this.mensagem = '';
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  deletar(id: number): void {
    if (!confirm('Deseja realmente excluir este cliente?')) return;
    this.service.deletar(id).subscribe({
      next: () => {
        this.clientes = this.clientes.filter(c => c.id !== id);
        this.alerta('Cliente excluído com sucesso!', 'success');
      },
      error: (err: HttpErrorResponse) => this.alerta(this.mensagemErro(err), 'danger')
    });
  }

  resetar(): void {
    this.form = this.formVazio();
    this.editandoId = null;
  }

  private formVazio(): Cliente {
    return { nome: '', email: '', telefone: '', ativo: true };
  }

  private alerta(texto: string, tipo: 'success' | 'danger'): void {
    this.mensagem = texto;
    this.tipoMensagem = tipo;
    setTimeout(() => (this.mensagem = ''), 5000);
  }

  private mensagemErro(err: HttpErrorResponse): string {
    if (typeof err.error === 'string') {
      try { return JSON.parse(err.error).erro ?? err.error; } catch { return err.error; }
    }
    return err.error?.erro ?? 'Ocorreu um erro inesperado.';
  }
}
