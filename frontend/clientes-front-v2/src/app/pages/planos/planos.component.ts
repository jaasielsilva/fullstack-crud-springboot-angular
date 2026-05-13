import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { environment } from '../../../environments/environment';
import { PlanoPublico } from '../../models/plano-public.model';
import { AuthService } from '../../security/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

type CheckoutGateway = 'mp' | 'abacate';

@Component({
  selector: 'app-planos',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './planos.component.html',
  styleUrl: './planos.component.css'
})
export class PlanosComponent implements OnInit {
  planos: PlanoPublico[] = [];
  carregandoLista = true;
  checkoutErro = '';
  /** Checkout em andamento: desabilita ambos os botões até redirect ou erro */
  checkoutEmAndamento: null | { gateway: CheckoutGateway; planoId: number } = null;

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void {
    this.http.get<PlanoPublico[]>(`${environment.apiUrl}/api/public/planos`).subscribe({
      next: (data) => {
        this.planos = data;
        this.carregandoLista = false;
      },
      error: () => {
        this.carregandoLista = false;
        this.checkoutErro = 'Não foi possível carregar os planos.';
      }
    });
  }

  assinarMercadoPago(planoId: number): void {
    this.iniciarCheckout(planoId, 'mp');
  }

  assinarAbacatePay(planoId: number): void {
    this.iniciarCheckout(planoId, 'abacate');
  }

  private iniciarCheckout(planoId: number, gateway: CheckoutGateway): void {
    const empresaId = this.auth.getTenantId();
    if (!empresaId) {
      this.checkoutErro = 'Sessão inválida. Faça login novamente.';
      return;
    }

    this.checkoutErro = '';
    this.checkoutEmAndamento = { gateway, planoId };

    const path =
      gateway === 'mp' ? `${environment.apiUrl}/api/public/checkout` : `${environment.apiUrl}/api/public/checkout/abacate`;

    this.http
      .post<{
        checkoutUrl?: string;
        preferenceId?: string;
        init_point?: string;
      }>(path, {
        empresaId,
        planoId
      })
      .subscribe({
        next: (res) => {
          const initPoint = res.checkoutUrl ?? res.init_point;
          if (!environment.production) {
            console.log(`[${gateway}] checkoutUrl`, initPoint);
          }
          if (initPoint) {
            window.location.href = initPoint;
          } else {
            this.checkoutErro = 'URL de checkout não retornada pelo servidor.';
            this.checkoutEmAndamento = null;
          }
        },
        error: (err: HttpErrorResponse) => {
          this.checkoutEmAndamento = null;
          if (err.error?.erro) {
            this.checkoutErro = err.error.erro;
          } else {
            this.checkoutErro = 'Não foi possível iniciar o checkout.';
          }
        }
      });
  }

  redirecionando(planoId: number, gateway: CheckoutGateway): boolean {
    return (
      this.checkoutEmAndamento !== null &&
      this.checkoutEmAndamento.planoId === planoId &&
      this.checkoutEmAndamento.gateway === gateway
    );
  }

  formatMoney(v: number): string {
    return v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  labelBotao(plano: PlanoPublico): string {
    if (plano.tipo === 'BASICO') {
      return 'Assinar Básico';
    }
    if (plano.tipo === 'PREMIUM') {
      return 'Assinar Premium';
    }
    return 'Assinar';
  }

  labelBotaoAbacate(plano: PlanoPublico): string {
    const base = this.labelBotao(plano).replace(/^Assinar\s+/, '');
    return `Abacate Pay — ${base}`;
  }
}
