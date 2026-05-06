package com.clientes_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clientes_api.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findFirstByEmail(String email);
}
