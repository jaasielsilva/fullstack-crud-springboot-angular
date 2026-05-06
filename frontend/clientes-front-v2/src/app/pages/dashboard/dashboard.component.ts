import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router'; // Necessário para os botões que mudam de página

// 1. Importamos os serviços que já buscam os dados na API
import { ClienteService } from '../clientes/cliente.service';
import { ProdutoService } from '../produtos/produto.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule], // Adicionamos aqui!
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  // Variáveis para guardar a quantidade (começam em 0)
  totalClientes: number = 0;
  totalProdutos: number = 0;
  carregando: boolean = true; // Para mostrar um "loading" enquanto carrega

  // 2. Injetamos os serviços aqui no construtor. É assim que o Angular sabe que precisamos usar eles.
  constructor(
    private clienteService: ClienteService,
    private produtoService: ProdutoService
  ) { }

  // 3. O 'ngOnInit' é executado automaticamente assim que a tela abre
  ngOnInit(): void {
    console.log('[Dashboard] Buscando dados...');
    this.carregarTotais();
  }

  // 4. Nossa função que chama o backend
  carregarTotais(): void {
    // Busca os clientes e pega o tamanho da lista (length)
    this.clienteService.listar().subscribe({
      next: (dados) => {
        this.totalClientes = dados.length;
        this.checarCarregamento();
      }
    });

    // Busca os produtos e pega o tamanho da lista (length)
    this.produtoService.listar().subscribe({
      next: (dados) => {
        this.totalProdutos = dados.length;
        this.checarCarregamento();
      }
    });
  }

  // Só tira o "loading" quando tiver carregado algo
  checarCarregamento() {
    this.carregando = false;
  }

}
