package com.clientes_api.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import com.clientes_api.dto.ClienteRequestDTO;
import com.clientes_api.dto.ClienteResponseDTO;
import com.clientes_api.model.Cliente;
import com.clientes_api.repository.ClienteRepository;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<ClienteResponseDTO> listarTodos() {
        return clienteRepository.findAll()
                .stream()
                .map(ClienteResponseDTO::from)
                .toList();
    }

    public ClienteResponseDTO buscarPorId(Long id) {
        return ClienteResponseDTO.from(encontrar(id));
    }

    @CacheEvict(value = "dashboardExecutivo", allEntries = true)
    public ClienteResponseDTO salvar(ClienteRequestDTO dto) {
        clienteRepository.findFirstByEmail(dto.getEmail()).ifPresent(c -> {
            throw new RuntimeException("Já existe um cliente cadastrado com esse email");
        });

        Cliente cliente = new Cliente();
        cliente.setNome(dto.getNome());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefone(dto.getTelefone());
        cliente.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        return ClienteResponseDTO.from(clienteRepository.save(cliente));
    }

    @CacheEvict(value = "dashboardExecutivo", allEntries = true)
    public ClienteResponseDTO atualizar(Long id, ClienteRequestDTO dto) {
        Cliente cliente = encontrar(id);

        if (!cliente.getEmail().equals(dto.getEmail())) {
            clienteRepository.findFirstByEmail(dto.getEmail()).ifPresent(c -> {
                throw new RuntimeException("Já existe um cliente cadastrado com esse email");
            });
        }

        cliente.setNome(dto.getNome());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefone(dto.getTelefone());
        cliente.setAtivo(dto.getAtivo());

        return ClienteResponseDTO.from(clienteRepository.save(cliente));
    }

    @CacheEvict(value = "dashboardExecutivo", allEntries = true)
    public void deletar(Long id) {
        clienteRepository.delete(encontrar(id));
    }

    private Cliente encontrar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
    }
}
