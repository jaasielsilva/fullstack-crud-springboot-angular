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
  
  private apiUrl = `${environment.apiUrl}/api/usuarios`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  isUsuarioLogado(login: string): boolean {
    return this.authService.getUserInfo().sub === login;
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

  limpar() {
    this.usuarioSelecionado = { login: '', username: '', senha: '', role: 'USER' };
    this.isEditando = false;
  }
}
