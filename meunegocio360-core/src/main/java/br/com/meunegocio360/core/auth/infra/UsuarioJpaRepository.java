package br.com.meunegocio360.core.auth.infra;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {
  Optional<UsuarioEntity> findByEmail(String email);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from UsuarioEntity u where u.email = :email")
  Optional<UsuarioEntity> buscarPorEmailComBloqueio(@Param("email") String email);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from UsuarioEntity u where u.id = :id")
  Optional<UsuarioEntity> buscarPorIdComBloqueio(@Param("id") UUID id);
}
