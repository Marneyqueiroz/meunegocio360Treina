package br.com.meunegocio360.core.auth.application;

import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class SeedDesenvolvimento implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  private final PasswordEncoder encoder;
  private final String adminSenha;
  private final String usuarioSenha;
  public SeedDesenvolvimento(JdbcTemplate jdbc, PasswordEncoder encoder,
      @Value("${SEED_ADMIN_SENHA:}") String adminSenha,
      @Value("${SEED_USUARIO_SENHA:}") String usuarioSenha) {
    this.jdbc = jdbc; this.encoder = encoder; this.adminSenha = adminSenha; this.usuarioSenha = usuarioSenha;
  }
  @Override public void run(ApplicationArguments args) {
    if (adminSenha.isBlank() || usuarioSenha.isBlank()) throw new IllegalStateException("SEED_ADMIN_SENHA e SEED_USUARIO_SENHA são obrigatórias no perfil dev.");
    inserir("Administrador", "admin@meunegocio360.com.br", adminSenha, "ADMIN");
    inserir("Usuário de demonstração", "usuario@meunegocio360.com.br", usuarioSenha, "USUARIO");
  }
  private void inserir(String nome, String email, String senha, String perfil) {
    Integer count = jdbc.queryForObject("select count(*) from usuario where email = ?", Integer.class, email);
    if (count != null && count > 0) return;
    Instant now = Instant.now();
    jdbc.update("insert into usuario (id,nome,email,senha_hash,perfil,ativo,tentativas_falhas,criado_em,atualizado_em,versao) values (?,?,?,?,?,true,0,?,?,0)",
        uuidBytes(UUID.randomUUID()), nome, email, encoder.encode(senha), perfil, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
  }
  private byte[] uuidBytes(UUID uuid) {
    return java.nio.ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
  }
}
