import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-configuracoes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './configuracoes.component.html',
  styleUrl: './configuracoes.component.css'
})
export class ConfiguracoesComponent implements OnInit {
  
  emailConfig: any = {
    host: 'smtp.gmail.com',
    port: 587,
    usuario: '',
    senha: '',
    protocol: 'smtp',
    auth: true,
    starttls: true
  };

  carregando = false;
  mensagemSucesso = '';
  mensagemErro = '';

  private apiUrl = 'http://localhost:8080/api/config/email';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.carregarConfiguracao();
  }

  carregarConfiguracao() {
    this.http.get(this.apiUrl).subscribe({
      next: (data: any) => {
        if (data) {
          this.emailConfig = data;
        }
      },
      error: (err) => {
        console.error('Erro ao carregar configurações', err);
      }
    });
  }

  salvarEmailConfig() {
    this.carregando = true;
    this.mensagemSucesso = '';
    this.mensagemErro = '';

    this.http.post(this.apiUrl, this.emailConfig).subscribe({
      next: (data: any) => {
        this.emailConfig = data;
        this.mensagemSucesso = 'Configurações de e-mail salvas com sucesso!';
        this.carregando = false;
        setTimeout(() => this.mensagemSucesso = '', 3000);
      },
      error: (err) => {
        this.mensagemErro = 'Erro ao salvar configurações de e-mail.';
        this.carregando = false;
        console.error(err);
      }
    });
  }
}
