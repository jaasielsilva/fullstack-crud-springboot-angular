package com.clientes_api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.clientes_api.dto.ProdutoRequestDTO;
import com.clientes_api.dto.ProdutoResponseDTO;
import com.clientes_api.model.Produto;
import com.clientes_api.repository.ProdutoRepository;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public List<ProdutoResponseDTO> listarTodos() {
        return produtoRepository.findAll()
                .stream()
                .map(ProdutoResponseDTO::from)
                .toList();
    }

    public ProdutoResponseDTO buscarPorId(Long id) {
        return ProdutoResponseDTO.from(encontrar(id));
    }

    public ProdutoResponseDTO salvar(ProdutoRequestDTO dto) {
        Produto produto = new Produto();
        produto.setNome(dto.getNome());
        produto.setDescricao(dto.getDescricao());
        produto.setPreco(dto.getPreco());
        produto.setQuantidade(dto.getQuantidade() != null ? dto.getQuantidade() : 0);
        produto.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        return ProdutoResponseDTO.from(produtoRepository.save(produto));
    }

    public ProdutoResponseDTO atualizar(Long id, ProdutoRequestDTO dto) {
        Produto produto = encontrar(id);

        produto.setNome(dto.getNome());
        produto.setDescricao(dto.getDescricao());
        produto.setPreco(dto.getPreco());
        produto.setQuantidade(dto.getQuantidade());
        produto.setAtivo(dto.getAtivo());

        return ProdutoResponseDTO.from(produtoRepository.save(produto));
    }

    public void deletar(Long id) {
        produtoRepository.delete(encontrar(id));
    }

    private Produto encontrar(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
    }
}
