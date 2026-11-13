package br.com.meunegocio360.core.auth.infra;

import br.com.meunegocio360.core.auth.domain.Perfil;
import br.com.meunegocio360.core.auth.domain.Usuario;
import br.com.meunegocio360.core.auth.domain.UsuarioPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UsuarioJpaAdapter implements UsuarioPort {
  private final UsuarioJpaRepository repository;
  public UsuarioJpaAdapter(UsuarioJpaRepository repository) { this.repository=repository; }
  @Override @Transactional
  public Optional<Usuario> buscarPorEmailComBloqueio(String email) { return repository.buscarPorEmailComBloqueio(email).map(this::toDomain); }
  @Override @Transactional
  public Optional<Usuario> buscarPorIdComBloqueio(UUID id) { return repository.buscarPorIdComBloqueio(id).map(this::toDomain); }
  @Override @Transactional(readOnly = true)
  public Optional<Usuario> buscarPorId(UUID id) { return repository.findById(id).map(this::toDomain); }
  @Override @Transactional
  public void salvar(Usuario usuario) {
    var entity=repository.findById(usuario.id()).orElseThrow();
    entity.atualizarAutenticacao(usuario.tentativasFalhas(), usuario.bloqueadoAte(), usuario.ultimoLoginEm(), Instant.now());
  }
  private Usuario toDomain(UsuarioEntity e) {
    return new Usuario(e.getId(),e.getNome(),e.getEmail(),e.getSenhaHash(),e.getPerfil(),e.isAtivo(),
        e.getTentativasFalhas(),e.getBloqueadoAte(),e.getUltimoLoginEm());
  }
}
