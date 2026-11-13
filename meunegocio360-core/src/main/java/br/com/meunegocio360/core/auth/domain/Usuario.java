package br.com.meunegocio360.core.auth.domain;

import java.time.Instant;
import java.util.UUID;

public class Usuario {
  private final UUID id;
  private final String nome;
  private final String email;
  private final String senhaHash;
  private final Perfil perfil;
  private final boolean ativo;
  private int tentativasFalhas;
  private Instant bloqueadoAte;
  private Instant ultimoLoginEm;
  public Usuario(UUID id, String nome, String email, String senhaHash, Perfil perfil, boolean ativo,
      int tentativasFalhas, Instant bloqueadoAte, Instant ultimoLoginEm) {
    this.id=id; this.nome=nome; this.email=email; this.senhaHash=senhaHash; this.perfil=perfil;
    this.ativo=ativo; this.tentativasFalhas=tentativasFalhas; this.bloqueadoAte=bloqueadoAte; this.ultimoLoginEm=ultimoLoginEm;
  }
  public UUID id() { return id; }
  public String nome() { return nome; }
  public String email() { return email; }
  public String senhaHash() { return senhaHash; }
  public Perfil perfil() { return perfil; }
  public boolean ativo() { return ativo; }
  public int tentativasFalhas() { return tentativasFalhas; }
  public Instant bloqueadoAte() { return bloqueadoAte; }
  public Instant ultimoLoginEm() { return ultimoLoginEm; }
  public void registrarFalha(Instant bloqueioAte) { tentativasFalhas++; if (bloqueioAte != null) this.bloqueadoAte=bloqueioAte; }
  public void registrarSucesso(Instant now) { tentativasFalhas=0; bloqueadoAte=null; ultimoLoginEm=now; }
}
