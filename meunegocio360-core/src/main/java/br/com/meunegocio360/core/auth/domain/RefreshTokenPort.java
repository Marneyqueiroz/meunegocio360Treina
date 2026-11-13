package br.com.meunegocio360.core.auth.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenPort {
  Optional<RefreshToken> buscarComBloqueio(String hash);
  void salvar(UUID id, UUID usuarioId, UUID familiaId, String hash, Instant expiresAt, Instant createdAt);
  void revogarFamilia(UUID family, Instant now);
  void revogarUsuario(UUID usuario, Instant now);
  void removerAntigos(Instant now, Instant cutoff);
}
