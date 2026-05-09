package com.clientes_api.repository;

import com.clientes_api.model.Plano;
import com.clientes_api.model.enums.TipoPlano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanoRepository extends JpaRepository<Plano, Long> {

    Optional<Plano> findByTipo(TipoPlano tipo);

    List<Plano> findByAtivoTrueOrderByValorAsc();
}
