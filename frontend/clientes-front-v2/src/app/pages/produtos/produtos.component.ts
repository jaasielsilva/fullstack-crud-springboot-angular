import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ProdutoService } from './produto.service';
import { Produto } from './produto.model';

@Component({
  selector: 'app-produtos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './produtos.component.html',
  styleUrl: './produtos.component.css'
})
export class ProdutosComponent implements OnInit {
  produtos: Produto[] = [];
  form: Produto = this.formVazio();
  editandoId: number | null = null;
  
  // UX State
  carregando = false;
  mostrarForm = false; // Controla o Offcanvas
  
  // Toast State
  mensagem = '';
  tipoMensagem: 'success' | 'danger' = 'success';

  constructor(private service: ProdutoService) {}

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.service.listar().subscribe({
      next: (data) => {
        this.produtos = data;
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
    if (!this.form.nome?.trim() || this.form.preco < 0) {
      this.alerta('Nome e preço válido são obrigatórios.', 'danger');
      return;
    }

    if (this.editandoId !== null) {
      this.service.atualizar(this.editandoId, this.form).subscribe({
        next: (atualizado) => {
          this.produtos = this.produtos.map(p => p.id === atualizado.id ? atualizado : p);
          this.alerta('Produto atualizado com sucesso!', 'success');
          this.fecharForm();
        },
        error: (err: HttpErrorResponse) => this.alerta(this.mensagemErro(err), 'danger')
      });
    } else {
      this.service.salvar(this.form).subscribe({
        next: (novo) => {
          this.produtos = [...this.produtos, novo];
          this.alerta('Produto cadastrado com sucesso!', 'success');
          this.fecharForm();
        },
        error: (err: HttpErrorResponse) => this.alerta(this.mensagemErro(err), 'danger')
      });
    }
  }

  editar(produto: Produto): void {
    this.editandoId = produto.id!;
    this.form = { ...produto };
    this.mensagem = '';
    this.mostrarForm = true; // Abre o painel
  }

  deletar(id: number): void {
    if (!confirm('Deseja realmente excluir este produto?')) return;
    this.service.deletar(id).subscribe({
      next: () => {
        this.produtos = this.produtos.filter(p => p.id !== id);
        this.alerta('Produto excluído com sucesso!', 'success');
      },
      error: (err: HttpErrorResponse) => this.alerta(this.mensagemErro(err), 'danger')
    });
  }

  resetar(): void {
    this.form = this.formVazio();
    this.editandoId = null;
  }

  private formVazio(): Produto {
    return { nome: '', descricao: '', preco: 0, quantidade: 0, ativo: true };
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
