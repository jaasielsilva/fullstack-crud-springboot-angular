import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ConfirmModalComponent } from '../../shared/components/confirm-modal/confirm-modal.component';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-empresas',
  standalone: true,
  imports: [CommonModule, FormsModule, ConfirmModalComponent],
  templateUrl: './empresas.component.html',
  styleUrl: './empresas.component.css'
})
export class EmpresasComponent implements OnInit {
  
  tenants: any[] = [];
  novoTenant: any = {
    nomeEmpresa: '',
    cnpj: '',
    emailAdmin: '',
    usernameAdmin: '',
    senhaAdmin: ''
  };
  
  mostrarForm = false;
  mostrarDetalhes = false;
  empresaSelecionada: any = null;
  statusMsg = '';
  statusType: 'success' | 'danger' = 'success';
  private apiUrl = `${environment.apiUrl}/api/tenants`;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.listar();
  }

  listar() {
    this.http.get<any[]>(this.apiUrl).subscribe(res => this.tenants = res);
  }

  abrirNovo() {
    this.novoTenant = { nomeEmpresa: '', cnpj: '', emailAdmin: '', usernameAdmin: '', senhaAdmin: '' };
    this.statusMsg = '';
    this.mostrarForm = true;
  }

  fecharForm() {
    this.mostrarForm = false;
  }

  verDetalhes(t: any) {
    this.empresaSelecionada = t;
    this.mostrarDetalhes = true;
  }

  fecharDetalhes() {
    this.mostrarDetalhes = false;
    this.empresaSelecionada = null;
  }

  salvar() {
    this.statusMsg = '';
    this.http.post(this.apiUrl, this.novoTenant).subscribe({
      next: () => {
        this.statusMsg = 'Empresa e Administrador cadastrados com sucesso!';
        this.statusType = 'success';
        this.listar();
        setTimeout(() => this.fecharForm(), 2000);
      },
      error: (err) => {
        this.statusMsg = 'Erro ao cadastrar: ' + (err.error?.erro || 'Verifique os dados e tente novamente.');
        this.statusType = 'danger';
      }
    });
  }

  formatarCNPJ() {
    let valor = this.novoTenant.cnpj.replace(/\D/g, ''); // Remove tudo que não é dígito
    
    if (valor.length > 14) {
      valor = valor.substring(0, 14);
    }

    // Aplica a máscara: 00.000.000/0000-00
    valor = valor.replace(/^(\d{2})(\d)/, '$1.$2');
    valor = valor.replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3');
    valor = valor.replace(/\.(\d{3})(\d)/, '.$1/$2');
    valor = valor.replace(/(\d{4})(\d)/, '$1-$2');

    this.novoTenant.cnpj = valor;
  }
}
