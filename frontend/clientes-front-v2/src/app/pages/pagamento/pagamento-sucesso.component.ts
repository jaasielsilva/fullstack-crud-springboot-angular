import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-pagamento-sucesso',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container py-5 text-center" style="max-width: 560px">
      <div class="display-4 text-success mb-3"><i class="bi bi-check-circle-fill"></i></div>
      <h1 class="h4 fw-bold">Pagamento recebido</h1>
      <ng-container *ngIf="fluxoMercadoPago; else textoAbacate">
        <p class="text-muted">
          Obrigado! O Mercado Pago está processando a confirmação.
          <strong>Seu acesso será liberado quando o webhook confirmar o pagamento como aprovado</strong> — não
          dependa apenas desta tela de retorno.
        </p>
      </ng-container>
      <ng-template #textoAbacate>
        <p class="text-muted">
          Obrigado! O <strong>Abacate Pay</strong> já registrou o pagamento nesta etapa.
          <strong>Seu acesso será liberado quando o nosso servidor confirmar o pagamento pelo webhook</strong> (pode levar
          alguns instantes) — não dependa apenas desta tela de retorno.
        </p>
        <p class="text-muted small mb-0">
          Enviaremos um <strong>comprovante por e-mail</strong> para o endereço da sua empresa ou da sua conta, assim que
          a confirmação for processada (é necessário ter o SMTP configurado no sistema).
        </p>
      </ng-template>
      <a routerLink="/dashboard" class="btn btn-primary mt-3">Ir ao painel</a>
    </div>
  `
})
export class PagamentoSucessoComponent implements OnInit {
  /** Retorno explícito do checkout Mercado Pago (`?gateway=mp`). Demais casos tratamos como Abacate Pay. */
  fluxoMercadoPago = false;

  constructor(private readonly route: ActivatedRoute) {}

  ngOnInit(): void {
    this.fluxoMercadoPago = this.route.snapshot.queryParamMap.get('gateway') === 'mp';
  }
}
