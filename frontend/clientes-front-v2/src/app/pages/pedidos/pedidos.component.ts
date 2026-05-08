import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { PedidoService } from './pedido.service';
import { Pedido, PedidoRequest } from './pedido.model';


@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pedidos.component.html',
  styleUrl: './pedidos.component.css'
})
export class PedidosComponent implements OnInit {

  pedidos: Pedido[] = [];

  carregando = false;

  mensagem = '';
  tipoMensagem: 'success' | 'danger' = 'success';

  constructor(private service: PedidoService) { }

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;

    this.service.listarPedidos().subscribe({
      next: (data: Pedido[]) => {
        this.pedidos = data;
        this.carregando = false;
      },
      error: (err: HttpErrorResponse) => {
        this.alerta('Não foi possível carregar os pedidos.', 'danger');
        this.carregando = false;
      }
    });
  }

  deletar(id: number): void {
    if (!confirm('Deseja realmente excluir este pedido?')) return;

    this.service.deletarPedido(id).subscribe({
      next: () => {
        this.pedidos = this.pedidos.filter(p => p.id !== id);
        this.alerta('Pedido excluído com sucesso!', 'success');
      },
      error: (err: HttpErrorResponse) => {
        this.alerta('Erro ao excluir pedido.', 'danger');
      }
    });
  }

  private alerta(texto: string, tipo: 'success' | 'danger'): void {
    this.mensagem = texto;
    this.tipoMensagem = tipo;

    setTimeout(() => {
      this.mensagem = '';
    }, 4000);
  }

  totalFaturado(): number {
    return this.pedidos.reduce((total, pedido) => total + (pedido.valorTotal || 0), 0);
  }

  mostrarForm = false;
  editandoId: number | null = null;
  clienteId: number | null = null;
  produtoId: number | null = null;
  quantidade = 1;
  salvando = false;

  abrirNovo(): void {
    this.editandoId = null;
    this.resetarFormulario();
    this.mostrarForm = true;
  }

  fecharForm(): void {
    this.mostrarForm = false;
    this.resetarFormulario();
  }

  editar(pedido: Pedido): void {
    if (!pedido.id) {
      this.alerta('Pedido inválido para edição.', 'danger');
      return;
    }

    this.editandoId = pedido.id;
    this.mostrarForm = true;
    this.salvando = true;

    this.service.buscarPorId(pedido.id).subscribe({
      next: (detalhe: Pedido) => {
        this.clienteId = detalhe.clienteId ?? null;

        const primeiroItem = detalhe.itens?.[0];
        this.produtoId = primeiroItem?.produtoId ?? null;
        this.quantidade = primeiroItem?.quantidade ?? 1;

        if ((detalhe.itens?.length ?? 0) > 1) {
          this.alerta('Pedido com múltiplos itens: editando o primeiro item no formulário.', 'danger');
        }

        this.salvando = false;
      },
      error: (_err: HttpErrorResponse) => {
        this.alerta('Não foi possível carregar os dados do pedido para edição.', 'danger');
        this.salvando = false;
      }
    });
  }

  salvarNovo(): void {
    if (!this.clienteId || !this.produtoId || this.quantidade <= 0) {
      this.fecharForm();
      this.alerta('Preencha Cliente, Produto e Quantidade corretamente.', 'danger');
      return;
    }

    const payload: PedidoRequest = {
      clienteId: this.clienteId,
      itens: [
        {
          produtoId: this.produtoId,
          quantidade: this.quantidade
        }
      ]
    };

    this.salvando = true;

    const requisicao = this.editandoId !== null
      ? this.service.atualizarPedido(this.editandoId, payload)
      : this.service.criarPedido(payload);

    requisicao.subscribe({
      next: () => {
        this.carregar();
        this.salvando = false;
        this.fecharForm();
        this.alerta(this.editandoId !== null ? 'Pedido atualizado com sucesso!' : 'Pedido criado com sucesso!', 'success');
      },
      error: (err: HttpErrorResponse) => {
        this.salvando = false;
        this.fecharForm();
        this.alerta(this.mensagemErro(err), 'danger');
      }
    });
  }

  private resetarFormulario(): void {
    this.editandoId = null;
    this.clienteId = null;
    this.produtoId = null;
    this.quantidade = 1;
  }

  private mensagemErro(err: HttpErrorResponse): string {
    if (typeof err.error === 'string') {
      try { return JSON.parse(err.error).erro ?? err.error; } catch { return err.error; }
    }
    return err.error?.erro ?? 'Ocorreu um erro inesperado.';
  }

}