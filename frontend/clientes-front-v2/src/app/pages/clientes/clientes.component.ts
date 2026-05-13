import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ClienteService } from './cliente.service';
import { Cliente } from './cliente.model';
import { ConfirmModalComponent } from '../../shared/components/confirm-modal/confirm-modal.component';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule, ConfirmModalComponent],
  templateUrl: './clientes.component.html',
  styleUrl: './clientes.component.css'
})
export class ClientesComponent implements OnInit {
  clientes: Cliente[] = [];
  form: Cliente = this.formVazio();
  editandoId: number | null = null;

  // UX State
  carregando = false;
  mostrarForm = false; // Controla o Offcanvas

  // Toast State
  mensagem = '';
  tipoMensagem: 'success' | 'danger' = 'success';

  showConfirmModal = false;
  idParaExcluir: number | null = null;

  constructor(private service: ClienteService) { }

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.service.listar().subscribe({
      next: (data) => {
        this.clientes = data;
        this.carregando = false;
      },
      error: (err) => {
        this.alerta('Não foi possível conectar ao servidor.', 'danger');
        this.carregando = false;
      }
    });
  }

  abrirNovo(): void {
    this.resetar();
    this.mostrarForm = true;
  }

  fecharForm(): void {
    this.mostrarForm = false;
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
          this.fecharForm();
        },
        error: (err: HttpErrorResponse) => this.alerta(this.mensagemErro(err), 'danger')
      });
    } else {
      this.service.salvar(this.form).subscribe({
        next: (novo) => {
          this.clientes = [...this.clientes, novo];
          this.alerta('Cliente cadastrado com sucesso!', 'success');
          this.fecharForm();
        },
        error: (err: HttpErrorResponse) => this.alerta(this.mensagemErro(err), 'danger')
      });
    }
  }

  editar(cliente: Cliente): void {
    this.editandoId = cliente.id!;
    this.form = { ...cliente };
    this.mensagem = '';
    this.mostrarForm = true; // Abre o painel
  }

  confirmarExcluir(id: number): void {
    this.idParaExcluir = id;
    this.showConfirmModal = true;
  }

  executarExclusao(): void {
    if (this.idParaExcluir == null) return;
    const id = this.idParaExcluir;
    this.service.deletar(id).subscribe({
      next: () => {
        this.clientes = this.clientes.filter(c => c.id !== id);
        this.alerta('Cliente excluído com sucesso!', 'success');
        this.showConfirmModal = false;
        this.idParaExcluir = null;
      },
      error: (err: HttpErrorResponse) => this.alerta(this.mensagemErro(err), 'danger')
    });
  }

  fecharModalExcluir(): void {
    this.showConfirmModal = false;
    this.idParaExcluir = null;
  }

  resetar(): void {
    this.form = this.formVazio();
    this.editandoId = null;
  }

  // Usada apenas na tela (tabela) para exibir o texto formatado
  formatarTelefone(valor: string | undefined | null): string {
    if (!valor) return 'Não informado';

    // Remove tudo que não for número
    let v = valor.replace(/\D/g, '');

    // Aplica a máscara
    if (v.length <= 10) {
      v = v.replace(/^(\d{2})(\d)/g, '($1) $2');
      v = v.replace(/(\d{4})(\d)/, '$1-$2');
    } else {
      v = v.replace(/^(\d{2})(\d)/g, '($1) $2');
      v = v.replace(/(\d{5})(\d)/, '$1-$2');
    }

    return v.substring(0, 15);
  }

  // Usada no formulário (ngModelChange) para formatar o que o usuário digita
  mascararTelefone(valor: string): void {
    if (!valor) {
      this.form.telefone = '';
      return;
    }
    this.form.telefone = this.formatarTelefone(valor);
  }
  private formVazio(): Cliente {
    return { nome: '', email: '', telefone: '', ativo: true };
  }

  private alerta(texto: string, tipo: 'success' | 'danger'): void {
    this.mensagem = texto;
    this.tipoMensagem = tipo;
    setTimeout(() => (this.mensagem = ''), 4000); // Some depois de 4s
  }

  private mensagemErro(err: HttpErrorResponse): string {
    if (typeof err.error === 'string') {
      try { return JSON.parse(err.error).erro ?? err.error; } catch { return err.error; }
    }
    return err.error?.erro ?? 'Ocorreu um erro inesperado.';
  }
}
