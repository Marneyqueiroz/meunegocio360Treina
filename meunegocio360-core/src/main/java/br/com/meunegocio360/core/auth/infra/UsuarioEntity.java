package br.com.meunegocio360.core.auth.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import br.com.meunegocio360.core.auth.domain.Perfil;

@Entity
@Table(name = "usuario")
public class UsuarioEntity {
  @Id private UUID id;
  @Column(nullable = false, length = 120) private String nome;
  @Column(nullable = false, length = 160, unique = true) private String email;
  @Column(name = "senha_hash", nullable = false, length = 100) private String senhaHash;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Perfil perfil;
  @Column(nullable = false) private boolean ativo;
  @Column(name = "tentativas_falhas", nullable = false) private int tentativasFalhas;
  @Column(name = "bloqueado_ate") private Instant bloqueadoAte;
  @Column(name = "ultimo_login_em") private Instant ultimoLoginEm;
  @Column(name = "criado_em", nullable = false) private Instant criadoEm;
  @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm;
  @Version private long versao;

  protected UsuarioEntity() {}

  public UUID getId() { return id; }
  public String getNome() { return nome; }
  public String getEmail() { return email; }
  public String getSenhaHash() { return senhaHash; }
  public Perfil getPerfil() { return perfil; }
  public boolean isAtivo() { return ativo; }
  public int getTentativasFalhas() { return tentativasFalhas; }
  public Instant getBloqueadoAte() { return bloqueadoAte; }
  public Instant getUltimoLoginEm() { return ultimoLoginEm; }
  public void registrarFalha(Instant agora, int maxTentativas, Instant bloqueioAte) {
    tentativasFalhas++;
    this.bloqueadoAte = tentativasFalhas >= maxTentativas ? bloqueioAte : this.bloqueadoAte;
    atualizadoEm = agora;
  }
  public void registrarSucesso(Instant agora) {
    tentativasFalhas = 0;
    bloqueadoAte = null;
    ultimoLoginEm = agora;
    atualizadoEm = agora;
  }
  public void atualizarAutenticacao(int tentativas, Instant bloqueio, Instant ultimoLogin, Instant agora) {
    this.tentativasFalhas = tentativas; this.bloqueadoAte = bloqueio;
    this.ultimoLoginEm = ultimoLogin; this.atualizadoEm = agora;
  }
}
