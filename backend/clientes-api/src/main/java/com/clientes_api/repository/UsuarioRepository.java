package com.clientes_api.repository;

import com.clientes_api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca padrão (Respeita o TenantId automático do Hibernate)
    Usuario findByLogin(String login);

    // Busca para Login (Ignora o TenantId para encontrar o usuário e seu respectivo tenant)
    @Query(value = "SELECT * FROM usuarios u WHERE u.login = :login OR u.username = :login LIMIT 1", nativeQuery = true)
    Usuario findByLoginOrUsernameGlobal(@Param("login") String login);

    // Busca apenas o tenant_id (sem materializar a entidade), útil quando o TenantContext ainda não está setado.
    @Query(value = "SELECT u.tenant_id FROM usuarios u WHERE LOWER(u.login) = LOWER(:login) OR LOWER(u.username) = LOWER(:login) LIMIT 1", nativeQuery = true)
    Long findTenantIdByLoginOrUsernameGlobal(@Param("login") String login);

    /** Verificação global de e-mail (cadastro trial / onboarding) sem filtro de tenant na sessão. */
    @Query(value = "SELECT COUNT(*) FROM usuarios u WHERE LOWER(u.login) = LOWER(:login)", nativeQuery = true)
    long countByLoginGlobal(@Param("login") String login);

    Usuario findByLoginOrUsername(String login, String username);

    Usuario findByResetToken(String resetToken);

    @Query("SELECT u FROM Usuario u WHERE u.resetToken = :token AND u.resetTokenExpiry > :agora")
    Optional<Usuario> findByResetTokenValido(@Param("token") String token, @Param("agora") LocalDateTime agora);

    Optional<Usuario> findFirstByTenantIdOrderByIdAsc(Long tenantId);

    List<Usuario> findAllByTenantIdOrderByLoginAsc(Long tenantId);

    Optional<Usuario> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByLoginIgnoreCaseAndTenantId(String login, Long tenantId);
}
