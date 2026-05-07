import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ConfirmModalComponent } from '../../shared/components/confirm-modal/confirm-modal.component';
import { AuthService } from '../../security/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, ConfirmModalComponent],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.css'
})
export class UsuariosComponent implements OnInit {
  
  usuarios: any[] = [];
  usuarioSelecionado: any = { login: '', username: '', senha: '', role: 'USER' };
  isEditando = false;
  mostrarForm = false; // Controle do Painel Lateral
  
  // Controle do Modal de Confirmação
  showConfirmModal = false;
  idParaExcluir: number | null = null;

  // Feedback rápido de troca de permissão
  mensagemSucesso = '';
  mensagemErro = '';
  idTrocandoRole: number | null = null;

  // Catálogo de roles disponíveis no sistema (espelha o enum do backend)
  roles = [
    { valor: 'ADMIN',    label: 'Administrador', descricao: 'Acesso total ao sistema',                  icone: 'bi-shield-lock-fill', classe: 'bg-danger-subtle text-danger' },
    { valor: 'GERENTE',  label: 'Gerente',       descricao: 'Visualiza tudo e gerencia operação',       icone: 'bi-briefcase-fill',   classe: 'bg-warning-subtle text-warning' },
    { valor: 'VENDEDOR', label: 'Vendedor',      descricao: 'Acesso a clientes, produtos e dashboard',  icone: 'bi-cart-check-fill',  classe: 'bg-success-subtle text-success' },
    { valor: 'SUPORTE',  label: 'Suporte',       descricao: 'Atendimento e consultas operacionais',     icone: 'bi-headset',          classe: 'bg-info-subtle text-info' },
    { valor: 'USER',     label: 'Sem permissão', descricao: 'Aguardando liberação do administrador',    icone: 'bi-person-fill',      classe: 'bg-secondary-subtle text-secondary' }
  ];
  
  private apiUrl = `${environment.apiUrl}/api/usuarios`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  isUsuarioLogado(login: string): boolean {
    return this.authService.getUserInfo().sub === login;
  }

  getRoleInfo(role: string) {
    return this.roles.find(r => r.valor === role) || this.roles[this.roles.length - 1];
  }

  ngOnInit(): void {
    this.listar();
  }

  listar() {
    this.http.get<any[]>(this.apiUrl).subscribe(res => this.usuarios = res);
  }

  abrirNovo() {
    this.limpar();
    this.mostrarForm = true;
  }

  fecharForm() {
    this.mostrarForm = false;
  }

  salvar() {
    if (this.isEditando) {
      this.http.put(`${this.apiUrl}/${this.usuarioSelecionado.id}`, this.usuarioSelecionado).subscribe(() => {
        this.listar();
        this.fecharForm();
      });
    } else {
      this.http.post(this.apiUrl, this.usuarioSelecionado).subscribe(() => {
        this.listar();
        this.fecharForm();
      });
    }
  }

  editar(u: any) {
    this.usuarioSelecionado = { ...u, senha: '' };
    this.isEditando = true;
    this.mostrarForm = true;
  }

  // Abre o modal em vez de usar confirm() nativo
  confirmarExcluir(id: number) {
    this.idParaExcluir = id;
    this.showConfirmModal = true;
  }

  // Executa a exclusão após confirmar no modal
  executarExclusao() {
    if (this.idParaExcluir) {
      this.http.delete(`${this.apiUrl}/${this.idParaExcluir}`).subscribe(() => {
        this.listar();
        this.showConfirmModal = false;
        this.idParaExcluir = null;
      });
    }
  }

  // Atalho usado pela linha da tabela para liberar/alterar a role rapidamente
  alterarRoleRapido(usuario: any, novaRole: string) {
    if (!novaRole || usuario.role === novaRole) return;

    this.idTrocandoRole = usuario.id;
    this.http.patch<any>(`${this.apiUrl}/${usuario.id}/role`, { role: novaRole }).subscribe({
      next: (resp) => {
        usuario.role = resp.role;
        this.idTrocandoRole = null;
        this.mostrarMensagem(`Permissão de ${usuario.username} atualizada para ${this.getRoleInfo(resp.role).label}.`, 'sucesso');
      },
      error: (err) => {
        this.idTrocandoRole = null;
        const msg = err?.error?.erro || 'Não foi possível alterar a permissão.';
        this.mostrarMensagem(msg, 'erro');
        this.listar();
      }
    });
  }

  private mostrarMensagem(texto: string, tipo: 'sucesso' | 'erro') {
    if (tipo === 'sucesso') {
      this.mensagemSucesso = texto;
      setTimeout(() => this.mensagemSucesso = '', 3500);
    } else {
      this.mensagemErro = texto;
      setTimeout(() => this.mensagemErro = '', 4500);
    }
  }

  limpar() {
    this.usuarioSelecionado = { login: '', username: '', senha: '', role: 'USER' };
    this.isEditando = false;
  }
}
