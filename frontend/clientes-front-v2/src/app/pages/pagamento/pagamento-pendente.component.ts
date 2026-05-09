import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-pagamento-pendente',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="container py-5 text-center" style="max-width: 560px">
      <div class="display-4 text-warning mb-3"><i class="bi bi-hourglass-split"></i></div>
      <h1 class="h4 fw-bold">Pagamento pendente</h1>
      <p class="text-muted">
        Seu pagamento está em análise ou aguardando confirmação (ex.: boleto ou revisão antifraude).
        Assim que o Mercado Pago notificar o sistema, atualizaremos sua assinatura automaticamente.
      </p>
      <a routerLink="/dashboard" class="btn btn-primary mt-3">Ir ao painel</a>
    </div>
  `
})
export class PagamentoPendenteComponent {}
