package com.clientes_api.service;

import com.clientes_api.exception.ResourceNotFoundException;
import com.clientes_api.model.Plano;
import com.clientes_api.repository.PlanoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlanoService {

    private final PlanoRepository planoRepository;

    public PlanoService(PlanoRepository planoRepository) {
        this.planoRepository = planoRepository;
    }

    public List<Plano> listarAtivosOrdenados() {
        return planoRepository.findByAtivoTrueOrderByValorAsc();
    }

    public Plano buscarPorIdOuErro(Long id) {
        return planoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado."));
    }
}
