import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { environment } from '../../../environments/environment';
import { PlanoPublico } from '../../models/plano-public.model';
import { AuthService } from '../../security/auth.service';

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
  /** Plano com checkout em andamento (até redirect ou erro). */
  planoIdEmCheckout: number | null = null;

  constructor(
    private readonly http: HttpClient,
    private readonly auth: AuthService
  ) {}

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

  contratar(planoId: number): void {
    const empresaId = this.auth.getTenantId();
    if (!empresaId) {
      this.checkoutErro = 'Sessão inválida. Faça login novamente.';
      return;
    }

    this.checkoutErro = '';
    this.planoIdEmCheckout = planoId;

    const url = `${environment.apiUrl}/api/public/checkout/abacate`;

    this.http
      .post<{
        checkoutUrl?: string;
        init_point?: string;
      }>(url, { empresaId, planoId })
      .subscribe({
        next: (res) => {
          const destino = res.checkoutUrl ?? res.init_point;
          if (!environment.production) {
            console.log('[checkout abacate]', destino);
          }
          if (destino) {
            window.location.href = destino;
          } else {
            this.checkoutErro = 'URL de checkout não retornada pelo servidor.';
            this.planoIdEmCheckout = null;
          }
        },
        error: (err: HttpErrorResponse) => {
          this.planoIdEmCheckout = null;
          this.checkoutErro =
            typeof err.error?.erro === 'string' ? err.error.erro : 'Não foi possível iniciar o checkout.';
        }
      });
  }

  checkoutEmAndamento(planoId: number): boolean {
    return this.planoIdEmCheckout === planoId;
  }

  formatMoney(v: number): string {
    return v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  /** Texto curto do CTA principal do card. */
  labelCta(plano: PlanoPublico): string {
    const nome = (plano.nome ?? '').trim();
    if (nome.length > 0) {
      return `Contratar — ${nome}`;
    }
    if (plano.tipo === 'BASICO') {
      return 'Contratar — Plano Básico';
    }
    if (plano.tipo === 'PREMIUM') {
      return 'Contratar — Plano Premium';
    }
    return 'Contratar plano';
  }

  trackByPlanoId(_index: number, plano: PlanoPublico): number {
    return plano.id;
  }
}
