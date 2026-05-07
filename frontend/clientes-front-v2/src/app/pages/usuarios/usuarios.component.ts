import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.css'
})
export class UsuariosComponent implements OnInit {
  
  usuarios: any[] = [];
  usuarioSelecionado: any = { login: '', username: '', senha: '', role: 'USER' };
  isEditando = false;
  mostrarForm = false; // Controle do Painel Lateral
  
  private apiUrl = 'http://localhost:8080/api/usuarios';

  constructor(private http: HttpClient) {}

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

  excluir(id: number) {
    if (confirm('Deseja realmente excluir este usuário?')) {
      this.http.delete(`${this.apiUrl}/${id}`).subscribe(() => this.listar());
    }
  }

  limpar() {
    this.usuarioSelecionado = { login: '', username: '', senha: '', role: 'USER' };
    this.isEditando = false;
  }
}
