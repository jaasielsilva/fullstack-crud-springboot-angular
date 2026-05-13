import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { finalize, switchMap, tap } from 'rxjs/operators';
import { PedidoService } from './pedido.service';
import { ClienteService } from '../clientes/cliente.service';
import { ProdutoService } from '../produtos/produto.service';
import { Pedido, PedidoRequest, ItemPedidoRequest } from './pedido.model';
import { Cliente } from '../clientes/cliente.model';
import { Produto } from '../produtos/produto.model';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../security/auth.service';
import { ConfirmModalComponent } from '../../shared/components/confirm-modal/confirm-modal.component';

export interface LinhaItemPedido {
  id: string;
  produtoId: number | null;
  quantidade: number;
}

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule, FormsModule, ConfirmModalComponent],
  templateUrl: './pedidos.component.html',
  styleUrl: './pedidos.component.css'
})
export class PedidosComponent implements OnInit {
  pedidos: Pedido[] = [];
  clientes: Cliente[] = [];
  produtos: Produto[] = [];

  carregando = false;
  carregandoCatalogo = false;

  mensagem = '';
  tipoMensagem: 'success' | 'danger' = 'success';

  mostrarForm = false;
  editandoId: number | null = null;
  salvando = false;

  clienteId: number | null = null;
  itensLinhas: LinhaItemPedido[] = [];

  showConfirmModal = false;
  confirmModalTitle = '';
  confirmModalMessage = '';
  confirmModalType: 'danger' | 'warning' | 'info' = 'warning';
  confirmModalConfirmText = 'Confirmar';
  private pendingConfirmAction: (() => void) | null = null;

  constructor(
    private service: PedidoService,
    private clienteService: ClienteService,
    private produtoService: ProdutoService,
    private auth: AuthService
  ) { }

  ngOnInit(): void {
    const qs = typeof window !== 'undefined' ? window.location.search : '';
    if (qs.includes('ap=success') || qs.includes('mp=success')) {
      this.alerta(
        'Se o pagamento foi aprovado, o status do pedido será atualizado em instantes.',
        'success'
      );
    }
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.service.listarPedidos().subscribe({
      next: (data: Pedido[]) => {
        this.pedidos = data;
        this.carregando = false;
      },
      error: (_err: HttpErrorResponse) => {
        this.alerta('Não foi possível carregar os pedidos.', 'danger');
        this.carregando = false;
      }
    });
  }

  deletar(id: number): void {
    this.abrirModalConfirmacao({
      title: 'Excluir pedido',
      message: 'Deseja realmente excluir este pedido? Esta ação não poderá ser desfeita.',
      type: 'danger',
      confirmText: 'Sim, excluir',
      onConfirm: () => this.executarExcluirPedido(id)
    });
  }

  private executarExcluirPedido(id: number): void {
    this.service.deletarPedido(id).subscribe({
      next: () => {
        this.pedidos = this.pedidos.filter(p => p.id !== id);
        this.alerta('Pedido excluído com sucesso!', 'success');
      },
      error: (_err: HttpErrorResponse) => {
        this.alerta('Erro ao excluir pedido.', 'danger');
      }
    });
  }

  podeSimularPagamento(): boolean {
    return (
      !!environment.simularPagamentoPedido &&
      this.auth.hasRole(['ADMIN', 'GERENTE'])
    );
  }

  statusBadgeClass(status: string | undefined): string {
    switch (status) {
      case 'ABERTO':
        return 'bg-warning-subtle text-dark';
      case 'PAGO':
        return 'bg-primary-subtle text-primary';
      case 'ENTREGUE':
        return 'bg-success-subtle text-success';
      case 'CANCELADO':
        return 'bg-secondary-subtle text-secondary';
      default:
        return 'bg-light text-muted';
    }
  }

  confirmarPagamento(pedido: Pedido): void {
    if (!pedido.id) return;
    this.abrirModalConfirmacao({
      title: 'Ir para pagamento',
      message: 'Você será redirecionado ao Abacate Pay para pagar este pedido com segurança.',
      type: 'info',
      confirmText: 'Continuar',
      onConfirm: () => this.executarCheckoutPedido(pedido)
    });
  }

  private executarCheckoutPedido(pedido: Pedido): void {
    if (!pedido.id) return;
    this.carregando = true;
    this.service.checkoutPedido(pedido.id).subscribe({
      next: (res) => {
        this.carregando = false;
        const initPoint = res.checkoutUrl ?? res.init_point;
        if (!environment.production) {
          console.log('init_point (checkoutUrl)', initPoint);
        }
        if (initPoint) {
          window.location.href = initPoint;
        } else {
          this.alerta('Resposta sem URL de checkout.', 'danger');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.carregando = false;
        this.alerta(this.mensagemErro(err), 'danger');
      }
    });
  }

  marcarEntregue(pedido: Pedido): void {
    if (!pedido.id) return;
    this.abrirModalConfirmacao({
      title: 'Marcar como entregue',
      message: 'Confirma que este pedido foi entregue ao cliente?',
      type: 'info',
      confirmText: 'Sim, entregue',
      onConfirm: () => this.executarMarcarEntregue(pedido)
    });
  }

  private executarMarcarEntregue(pedido: Pedido): void {
    if (!pedido.id) return;
    this.carregando = true;
    this.service.marcarEntregue(pedido.id).subscribe({
      next: (atual) => {
        this.substituirPedidoNaLista(atual);
        this.carregando = false;
        this.alerta('Pedido marcado como entregue.', 'success');
      },
      error: (err: HttpErrorResponse) => {
        this.carregando = false;
        this.alerta(this.mensagemErro(err), 'danger');
      }
    });
  }

  cancelarPedido(pedido: Pedido): void {
    if (!pedido.id) return;
    this.abrirModalConfirmacao({
      title: 'Cancelar pedido',
      message: 'O estoque dos itens será devolvido. Deseja cancelar este pedido?',
      type: 'warning',
      confirmText: 'Sim, cancelar',
      onConfirm: () => this.executarCancelarPedido(pedido)
    });
  }

  private executarCancelarPedido(pedido: Pedido): void {
    if (!pedido.id) return;
    this.carregando = true;
    this.service.cancelarPedido(pedido.id).subscribe({
      next: (atual) => {
        this.substituirPedidoNaLista(atual);
        this.carregando = false;
        this.alerta('Pedido cancelado.', 'success');
      },
      error: (err: HttpErrorResponse) => {
        this.carregando = false;
        this.alerta(this.mensagemErro(err), 'danger');
      }
    });
  }

  simularPagamento(pedido: Pedido): void {
    if (!pedido.id) return;
    this.abrirModalConfirmacao({
      title: 'Simular pagamento',
      message: 'Confirmar pagamento aprovado para este pedido? Só use quando a simulação estiver habilitada na API.',
      type: 'warning',
      confirmText: 'Confirmar',
      onConfirm: () => this.executarSimularPagamento(pedido)
    });
  }

  private executarSimularPagamento(pedido: Pedido): void {
    if (!pedido.id) return;
    this.carregando = true;
    this.service.simularPagamento(pedido.id).subscribe({
      next: (atual) => {
        this.substituirPedidoNaLista(atual);
        this.carregando = false;
        this.alerta('Pedido marcado como pago (simulação).', 'success');
      },
      error: (err: HttpErrorResponse) => {
        this.carregando = false;
        this.alerta(this.mensagemErro(err), 'danger');
      }
    });
  }

  private abrirModalConfirmacao(opts: {
    title: string;
    message: string;
    type?: 'danger' | 'warning' | 'info';
    confirmText?: string;
    onConfirm: () => void;
  }): void {
    this.confirmModalTitle = opts.title;
    this.confirmModalMessage = opts.message;
    this.confirmModalType = opts.type ?? 'warning';
    this.confirmModalConfirmText = opts.confirmText ?? 'Confirmar';
    this.pendingConfirmAction = opts.onConfirm;
    this.showConfirmModal = true;
  }

  onConfirmModal(): void {
    const fn = this.pendingConfirmAction;
    this.pendingConfirmAction = null;
    this.showConfirmModal = false;
    fn?.();
  }

  onCancelModal(): void {
    this.pendingConfirmAction = null;
    this.showConfirmModal = false;
  }

  private substituirPedidoNaLista(atual: Pedido): void {
    const idx = this.pedidos.findIndex((p) => p.id === atual.id);
    if (idx >= 0) {
      this.pedidos = [...this.pedidos.slice(0, idx), atual, ...this.pedidos.slice(idx + 1)];
    } else {
      this.carregar();
    }
  }

  totalFaturado(): number {
    return this.pedidos.reduce((total, pedido) => total + (pedido.valorTotal || 0), 0);
  }

  abrirNovo(): void {
    this.editandoId = null;
    this.resetarFormulario();
    this.mostrarForm = true;
    this.carregarCatalogo();
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
    this.carregandoCatalogo = true;

    this.carregarCatalogo$()
      .pipe(
        switchMap(() => this.service.buscarPorId(pedido.id!)),
        finalize(() => {
          this.carregandoCatalogo = false;
          this.salvando = false;
        })
      )
      .subscribe({
        next: (detalhe: Pedido) => {
          this.clienteId = detalhe.clienteId ?? null;
          const itens = detalhe.itens ?? [];
          this.itensLinhas =
            itens.length > 0
              ? itens.map((it, idx) => ({
                id: this.novoIdLinha(`ed-${idx}`),
                produtoId: it.produtoId ?? null,
                quantidade: Math.max(1, it.quantidade ?? 1)
              }))
              : [this.linhaVazia()];
        },
        error: (_err: HttpErrorResponse) => {
          this.alerta('Não foi possível carregar os dados do pedido para edição.', 'danger');
          this.fecharForm();
        }
      });
  }

  adicionarLinha(): void {
    this.itensLinhas = [...this.itensLinhas, this.linhaVazia()];
  }

  removerLinha(index: number): void {
    if (this.itensLinhas.length <= 1) {
      this.itensLinhas = [this.linhaVazia()];
      return;
    }
    this.itensLinhas = this.itensLinhas.filter((_, i) => i !== index);
  }

  produtoPorId(id: number | null): Produto | undefined {
    if (id == null) return undefined;
    return this.produtos.find(p => p.id === id);
  }

  subtotalLinha(linha: LinhaItemPedido): number {
    const p = this.produtoPorId(linha.produtoId);
    if (!p || linha.quantidade < 1) return 0;
    return (p.preco ?? 0) * linha.quantidade;
  }

  totalPedidoPreview(): number {
    return this.itensLinhas.reduce((acc, l) => acc + this.subtotalLinha(l), 0);
  }

  estoqueDisponivel(produtoId: number | null): number {
    const p = this.produtoPorId(produtoId);
    return p?.quantidade ?? 0;
  }

  estoqueOkParaLinha(linha: LinhaItemPedido): boolean {
    if (linha.produtoId == null || linha.quantidade < 1) return true;
    return this.estoqueDisponivel(linha.produtoId) >= linha.quantidade;
  }

  compareClienteId(o1: number | null, o2: number | null): boolean {
    return o1 === o2;
  }

  compareProdutoId(o1: number | null, o2: number | null): boolean {
    return o1 === o2;
  }

  trackLinha(_idx: number, linha: LinhaItemPedido): string {
    return linha.id;
  }

  salvarNovo(): void {
    if (!this.clienteId) {
      this.alerta('Selecione o cliente.', 'danger');
      return;
    }

    const itensPayload = this.montarItensParaSalvar();
    if (itensPayload.length === 0) {
      this.alerta('Inclua pelo menos um produto com quantidade válida.', 'danger');
      return;
    }

    if (this.editandoId === null) {
      for (const it of itensPayload) {
        const p = this.produtoPorId(it.produtoId);
        if (!p || (p.quantidade ?? 0) < it.quantidade) {
          this.alerta(
            `Estoque insuficiente para "${p?.nome ?? 'produto selecionado'}". Ajuste a quantidade.`,
            'danger'
          );
          return;
        }
      }
    }

    const payload: PedidoRequest = {
      clienteId: this.clienteId,
      itens: itensPayload
    };

    this.salvando = true;

    const requisicao =
      this.editandoId !== null
        ? this.service.atualizarPedido(this.editandoId, payload)
        : this.service.criarPedido(payload);

    requisicao.subscribe({
      next: () => {
        this.carregar();
        this.salvando = false;
        this.fecharForm();
        this.alerta(
          this.editandoId !== null ? 'Pedido atualizado com sucesso!' : 'Pedido criado com sucesso!',
          'success'
        );
      },
      error: (err: HttpErrorResponse) => {
        this.salvando = false;
        this.alerta(this.mensagemErro(err), 'danger');
      }
    });
  }

  private carregarCatalogo(): void {
    this.carregandoCatalogo = true;
    this.carregarCatalogo$()
      .pipe(finalize(() => (this.carregandoCatalogo = false)))
      .subscribe({
        error: (_err: HttpErrorResponse) => {
          this.alerta('Não foi possível carregar clientes ou produtos. Verifique a API.', 'danger');
        }
      });
  }

  private carregarCatalogo$() {
    return forkJoin({
      clientes: this.clienteService.listar(),
      produtos: this.produtoService.listar()
    }).pipe(
      tap(({ clientes, produtos }) => {
        this.clientes = [...clientes]
          .filter(c => c.ativo !== false)
          .sort((a, b) => (a.nome || '').localeCompare(b.nome || '', 'pt-BR'));
        this.produtos = [...produtos]
          .filter(p => p.ativo !== false)
          .sort((a, b) => (a.nome || '').localeCompare(b.nome || '', 'pt-BR'));
      })
    );
  }

  private montarItensParaSalvar(): ItemPedidoRequest[] {
    const mapa = new Map<number, number>();
    for (const linha of this.itensLinhas) {
      if (linha.produtoId == null || !linha.quantidade || linha.quantidade < 1) {
        continue;
      }
      mapa.set(linha.produtoId, (mapa.get(linha.produtoId) ?? 0) + linha.quantidade);
    }
    return Array.from(mapa.entries()).map(([produtoId, quantidade]) => ({ produtoId, quantidade }));
  }

  private resetarFormulario(): void {
    this.editandoId = null;
    this.clienteId = null;
    this.itensLinhas = [this.linhaVazia()];
    this.salvando = false;
  }

  private linhaVazia(): LinhaItemPedido {
    return { id: this.novoIdLinha('n'), produtoId: null, quantidade: 1 };
  }

  private novoIdLinha(prefix: string): string {
    const rnd =
      typeof globalThis.crypto !== 'undefined' && 'randomUUID' in globalThis.crypto
        ? globalThis.crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
    return `${prefix}-${rnd}`;
  }

  private alerta(texto: string, tipo: 'success' | 'danger'): void {
    this.mensagem = texto;
    this.tipoMensagem = tipo;
    setTimeout(() => {
      this.mensagem = '';
    }, 4500);
  }

  private mensagemErro(err: HttpErrorResponse): string {
    if (err.status === 404) {
      const body = err.error as { erro?: string } | null;
      return body?.erro ?? 'Registro não encontrado.';
    }
    if (typeof err.error === 'string') {
      try {
        return JSON.parse(err.error).erro ?? err.error;
      } catch {
        return err.error;
      }
    }
    if (err.error && typeof err.error === 'object') {
      const body = err.error as { erro?: string; campos?: Record<string, string> };
      if (body.campos && Object.keys(body.campos).length > 0) {
        const msgs = Object.values(body.campos).filter(Boolean);
        if (msgs.length) {
          return body.erro ? `${body.erro}: ${msgs.join(' ')}` : msgs.join(' ');
        }
      }
      if (body.erro) {
        return body.erro;
      }
    }
    return 'Ocorreu um erro inesperado. Tente novamente.';
  }
}
