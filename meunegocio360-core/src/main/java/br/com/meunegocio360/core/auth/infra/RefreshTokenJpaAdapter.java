package br.com.meunegocio360.core.auth.infra;

import br.com.meunegocio360.core.auth.domain.RefreshToken;
import br.com.meunegocio360.core.auth.domain.RefreshTokenPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RefreshTokenJpaAdapter implements RefreshTokenPort {
  private final RefreshTokenJpaRepository repository;
  public RefreshTokenJpaAdapter(RefreshTokenJpaRepository repository) { this.repository=repository; }
  @Override @Transactional
  public Optional<RefreshToken> buscarComBloqueio(String hash) {
    return repository.buscarComBloqueio(hash).map(t -> new RefreshToken(t.getUsuarioId(), t.getFamiliaId(), t.getExpiraEm(), t.getRevogadoEm()));
  }
  @Override public void salvar(UUID id, UUID userId, UUID family, String hash, Instant expiry, Instant created) {
    repository.save(new RefreshTokenEntity(id,userId,family,hash,expiry,created));
  }
  @Override public void revogarFamilia(UUID family, Instant now) { repository.revogarFamilia(family,now); }
  @Override public void revogarUsuario(UUID userId, Instant now) { repository.revogarUsuario(userId,now); }
  @Override public void removerAntigos(Instant now, Instant cutoff) { repository.removerAntigos(now,cutoff); }
}
