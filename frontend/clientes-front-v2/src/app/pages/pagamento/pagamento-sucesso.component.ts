import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-pagamento-sucesso',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="container py-5 text-center" style="max-width: 560px">
      <div class="display-4 text-success mb-3"><i class="bi bi-check-circle-fill"></i></div>
      <h1 class="h4 fw-bold">Pagamento recebido</h1>
      <p class="text-muted">
        Obrigado! O Mercado Pago está processando a confirmação.
        <strong>Seu acesso será liberado quando o webhook confirmar o pagamento como aprovado</strong> — não dependa apenas desta tela de retorno.
      </p>
      <a routerLink="/dashboard" class="btn btn-primary mt-3">Ir ao painel</a>
    </div>
  `
})
export class PagamentoSucessoComponent {}
