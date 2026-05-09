import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-pagamento-falha',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="container py-5 text-center" style="max-width: 560px">
      <div class="display-4 text-danger mb-3"><i class="bi bi-x-circle-fill"></i></div>
      <h1 class="h4 fw-bold">Pagamento não concluído</h1>
      <p class="text-muted">Não foi possível concluir o pagamento. Você pode tentar novamente escolhendo um plano.</p>
      <a routerLink="/planos" class="btn btn-outline-primary mt-3">Voltar aos planos</a>
    </div>
  `
})
export class PagamentoFalhaComponent {}
