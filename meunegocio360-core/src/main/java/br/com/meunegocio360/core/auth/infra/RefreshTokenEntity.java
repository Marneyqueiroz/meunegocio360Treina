package br.com.meunegocio360.core.auth.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshTokenEntity {
  @Id private UUID id;
  @Column(name = "usuario_id", nullable = false) private UUID usuarioId;
  @Column(name = "familia_id", nullable = false) private UUID familiaId;
  @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
  @Column(name = "expira_em", nullable = false) private Instant expiraEm;
  @Column(name = "revogado_em") private Instant revogadoEm;
  @Column(name = "criado_em", nullable = false) private Instant criadoEm;

  protected RefreshTokenEntity() {}
  public RefreshTokenEntity(UUID id, UUID usuarioId, UUID familiaId, String tokenHash, Instant expiraEm, Instant criadoEm) {
    this.id = id; this.usuarioId = usuarioId; this.familiaId = familiaId;
    this.tokenHash = tokenHash; this.expiraEm = expiraEm; this.criadoEm = criadoEm;
  }
  public UUID getUsuarioId() { return usuarioId; }
  public UUID getFamiliaId() { return familiaId; }
  public Instant getExpiraEm() { return expiraEm; }
  public Instant getRevogadoEm() { return revogadoEm; }
  public void revogar(Instant instante) { revogadoEm = instante; }
}
