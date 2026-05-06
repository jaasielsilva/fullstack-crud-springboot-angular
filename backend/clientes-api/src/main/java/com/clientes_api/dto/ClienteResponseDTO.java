package com.clientes_api.dto;

import com.clientes_api.model.Cliente;
import lombok.Data;

@Data
public class ClienteResponseDTO {

    private Long id;
    private String nome;
    private String email;
    private String telefone;
    private Boolean ativo;

    public static ClienteResponseDTO from(Cliente cliente) {
        ClienteResponseDTO dto = new ClienteResponseDTO();
        dto.id = cliente.getId();
        dto.nome = cliente.getNome();
        dto.email = cliente.getEmail();
        dto.telefone = cliente.getTelefone();
        dto.ativo = cliente.getAtivo();
        return dto;
    }
}
