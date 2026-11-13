package br.com.meunegocio360.core.auth.domain;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioPort {
  Optional<Usuario> buscarPorEmailComBloqueio(String email);
  Optional<Usuario> buscarPorIdComBloqueio(UUID id);
  Optional<Usuario> buscarPorId(UUID id);
  void salvar(Usuario usuario);
}
