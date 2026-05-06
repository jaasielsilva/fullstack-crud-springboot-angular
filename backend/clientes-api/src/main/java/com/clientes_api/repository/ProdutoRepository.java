package com.clientes_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clientes_api.model.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
