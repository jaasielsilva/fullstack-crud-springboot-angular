package com.clientes_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.clientes_api.model.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findAllByTenantId(Long tenantId);

    long countByTenantId(Long tenantId);

    @Query("SELECT COUNT(p) FROM Produto p WHERE p.tenantId = :tenantId AND (p.ativo IS NULL OR p.ativo = true)")
    long countAtivosByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(p) FROM Produto p WHERE p.tenantId = :tenantId AND p.quantidade IS NOT NULL AND p.quantidade > 0 AND p.quantidade <= 10")
    long countBaixoEstoqueByTenantId(@Param("tenantId") Long tenantId);

    Optional<Produto> findByIdAndTenantId(Long id, Long tenantId);
}
