import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { environment } from '../../../environments/environment';
import { PlanoPublico } from '../../models/plano-public.model';
import { AuthService } from '../../security/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

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
  contratandoId: number | null = null;

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

  assinar(planoId: number): void {
    const empresaId = this.auth.getTenantId();
    if (!empresaId) {
      this.checkoutErro = 'Sessão inválida. Faça login novamente.';
      return;
    }

    this.checkoutErro = '';
    this.contratandoId = planoId;

    this.http
      .post<{ checkoutUrl: string; preferenceId: string }>(`${environment.apiUrl}/api/public/checkout`, {
        empresaId,
        planoId
      })
      .subscribe({
        next: (res) => {
          if (res.checkoutUrl) {
            window.location.href = res.checkoutUrl;
          } else {
            this.checkoutErro = 'URL de checkout não retornada pelo servidor.';
            this.contratandoId = null;
          }
        },
        error: (err: HttpErrorResponse) => {
          this.contratandoId = null;
          if (err.error?.erro) {
            this.checkoutErro = err.error.erro;
          } else {
            this.checkoutErro = 'Não foi possível iniciar o checkout.';
          }
        }
      });
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
}
