package com.clientes_api.repository;

import com.clientes_api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca padrão (Respeita o TenantId automático do Hibernate)
    Usuario findByLogin(String login);

    // Busca para Login (Ignora o TenantId para encontrar o usuário e seu respectivo tenant)
    @Query(value = "SELECT * FROM usuarios u WHERE u.login = :login OR u.username = :login LIMIT 1", nativeQuery = true)
    Usuario findByLoginOrUsernameGlobal(@Param("login") String login);

    Usuario findByLoginOrUsername(String login, String username);

    Usuario findByResetToken(String resetToken);
}
