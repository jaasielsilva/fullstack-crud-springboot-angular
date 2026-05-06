package com.clientes_api.dto;

import com.clientes_api.model.Produto;
import lombok.Data;

@Data
public class ProdutoResponseDTO {

    private Long id;
    private String nome;
    private String descricao;
    private Double preco;
    private Integer quantidade;
    private Boolean ativo;

    public static ProdutoResponseDTO from(Produto produto) {
        ProdutoResponseDTO dto = new ProdutoResponseDTO();
        dto.id = produto.getId();
        dto.nome = produto.getNome();
        dto.descricao = produto.getDescricao();
        dto.preco = produto.getPreco();
        dto.quantidade = produto.getQuantidade();
        dto.ativo = produto.getAtivo();
        return dto;
    }
}
