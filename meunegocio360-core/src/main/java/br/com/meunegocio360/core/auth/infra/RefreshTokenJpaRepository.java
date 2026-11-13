package br.com.meunegocio360.core.auth.infra;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from RefreshTokenEntity t where t.tokenHash = :hash")
  Optional<RefreshTokenEntity> buscarComBloqueio(@Param("hash") String hash);
  @Modifying
  @Query("update RefreshTokenEntity t set t.revogadoEm = :agora where t.familiaId = :familia and t.revogadoEm is null")
  void revogarFamilia(@Param("familia") UUID familia, @Param("agora") Instant agora);
  @Modifying
  @Query("delete from RefreshTokenEntity t where t.expiraEm < :agora or t.revogadoEm < :limite")
  void removerAntigos(@Param("agora") Instant agora, @Param("limite") Instant limite);
  List<RefreshTokenEntity> findByUsuarioIdAndRevogadoEmIsNull(UUID usuarioId);
  @Modifying
  @Query("update RefreshTokenEntity t set t.revogadoEm = :agora where t.usuarioId = :usuario and t.revogadoEm is null")
  void revogarUsuario(@Param("usuario") UUID usuario, @Param("agora") Instant agora);
}
