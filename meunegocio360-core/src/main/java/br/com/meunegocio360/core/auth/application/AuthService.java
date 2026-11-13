package br.com.meunegocio360.core.auth.application;

import br.com.meunegocio360.core.auth.api.LoginRequest;
import br.com.meunegocio360.core.auth.api.SessaoResponse;
import br.com.meunegocio360.core.auth.api.UsuarioResponse;
import br.com.meunegocio360.core.auth.domain.RefreshToken;
import br.com.meunegocio360.core.auth.domain.RefreshTokenPort;
import br.com.meunegocio360.core.auth.domain.Usuario;
import br.com.meunegocio360.core.auth.domain.UsuarioPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private final UsuarioPort usuarios;
  private final RefreshTokenPort refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final JwtEncoder jwtEncoder;
  private final SecureRandom random = new SecureRandom();
  private final Clock clock;
  private final String hashFicticio;
  private final String issuer;
  private final int maxTentativas;
  private final int bloqueioMinutos;
  private final int expiracaoMinutos;
  private final int refreshDias;

  public AuthService(UsuarioPort usuarios, RefreshTokenPort refreshTokens,
      PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder, Clock clock,
      @Value("${app.auth.jwt.issuer}") String issuer,
      @Value("${app.auth.max-tentativas}") int maxTentativas,
      @Value("${app.auth.bloqueio-minutos}") int bloqueioMinutos,
      @Value("${app.auth.jwt.expiracao-minutos}") int expiracaoMinutos,
      @Value("${app.auth.refresh.expiracao-dias}") int refreshDias) {
    this.usuarios = usuarios; this.refreshTokens = refreshTokens; this.passwordEncoder = passwordEncoder;
    this.jwtEncoder = jwtEncoder; this.clock = clock; this.issuer = issuer;
    this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
    this.maxTentativas = maxTentativas; this.bloqueioMinutos = bloqueioMinutos;
    this.expiracaoMinutos = expiracaoMinutos; this.refreshDias = refreshDias;
  }

  @Transactional(noRollbackFor = AuthException.class)
  public SessaoCriada login(LoginRequest request) {
    Instant now = clock.instant();
    String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
    Usuario user = usuarios.buscarPorEmailComBloqueio(email).orElse(null);
    if (user == null) {
      passwordEncoder.matches(request.senha(), hashFicticio);
      log.info("auth_login_failure reason=unknown_user");
      throw new AuthException("INVALID_CREDENTIALS", 401);
    }
    if (user.bloqueadoAte() != null && user.bloqueadoAte().isAfter(now)) {
      log.info("auth_login_blocked");
      throw new AuthException("ACCOUNT_TEMPORARILY_LOCKED", 429,
          Math.max(1, Duration.between(now, user.bloqueadoAte()).toSeconds()));
    }
    boolean senhaValida = passwordEncoder.matches(request.senha(), user.senhaHash());
    if (!user.ativo()) { log.info("auth_login_failure reason=inactive_user"); throw new AuthException("INVALID_CREDENTIALS", 401); }
    if (!senhaValida) {
      user.registrarFalha(user.tentativasFalhas() + 1 >= maxTentativas ? now.plus(Duration.ofMinutes(bloqueioMinutos)) : null);
      usuarios.salvar(user);
      if (user.tentativasFalhas() >= maxTentativas) {
        refreshTokens.revogarUsuario(user.id(), now);
        log.info("auth_login_blocked");
        throw new AuthException("ACCOUNT_TEMPORARILY_LOCKED", 429, Duration.ofMinutes(bloqueioMinutos).toSeconds());
      }
      log.info("auth_login_failure reason=invalid_credentials");
      throw new AuthException("INVALID_CREDENTIALS", 401);
    }
    user.registrarSucesso(now);
    usuarios.salvar(user);
    log.info("auth_login_success");
    UUID family = UUID.randomUUID();
    return criarSessao(user, family, now);
  }

  @Transactional(noRollbackFor = AuthException.class)
  public SessaoCriada renovar(String refreshToken) {
    Instant now = clock.instant();
    if (refreshToken == null || refreshToken.isBlank()) throw invalidRefresh();
    RefreshToken token = refreshTokens.buscarComBloqueio(hash(refreshToken)).orElseThrow(AuthService::invalidRefresh);
    if (token.revogadoEm() != null) {
      refreshTokens.revogarFamilia(token.familiaId(), now);
      throw invalidRefresh();
    }
    if (!token.expiraEm().isAfter(now)) { refreshTokens.revogarFamilia(token.familiaId(), now); throw invalidRefresh(); }
    Usuario user = usuarios.buscarPorIdComBloqueio(token.usuarioId()).orElseThrow(AuthService::invalidRefresh);
    if (!user.ativo() || (user.bloqueadoAte() != null && user.bloqueadoAte().isAfter(now))) {
      refreshTokens.revogarFamilia(token.familiaId(), now);
      throw invalidRefresh();
    }
    log.info("auth_refresh_success");
    refreshTokens.revogarFamilia(token.familiaId(), now);
    return criarSessao(user, token.familiaId(), now);
  }

  @Transactional
  public void logout(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) return;
    refreshTokens.buscarComBloqueio(hash(refreshToken)).ifPresent(token -> refreshTokens.revogarFamilia(token.familiaId(), clock.instant()));
  }

  @Transactional(readOnly = true)
  public UsuarioResponse usuarioAutenticado(UUID id) {
    Usuario user = usuarios.buscarPorId(id).orElseThrow(() -> new AuthException("UNAUTHORIZED", 401));
    if (!user.ativo()) throw new AuthException("UNAUTHORIZED", 401);
    return response(user);
  }

  private SessaoCriada criarSessao(Usuario user, UUID family, Instant now) {
    Instant expira = now.plus(Duration.ofDays(refreshDias));
    byte[] bytes = new byte[32]; random.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    refreshTokens.salvar(UUID.randomUUID(), user.id(), family, hash(raw), expira, now);
    Instant accessExp = now.plus(Duration.ofMinutes(expiracaoMinutos));
    JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(user.id().toString())
        .issuedAt(now).expiresAt(accessExp).id(UUID.randomUUID().toString()).claim("perfil", user.perfil().name()).build();
    String access = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    return new SessaoCriada(new SessaoResponse("Bearer", access, Duration.between(now, accessExp).toSeconds(), response(user)), raw, expira);
  }
  private UsuarioResponse response(Usuario user) { return new UsuarioResponse(user.id(), user.nome(), user.email(), user.perfil().name()); }
  private static AuthException invalidRefresh() { return new AuthException("INVALID_REFRESH_TOKEN", 401); }
  private static String hash(String token) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
    catch (Exception ex) { throw new IllegalStateException(ex); }
  }
  public record SessaoCriada(SessaoResponse response, String refreshToken, Instant refreshExpiraEm) {}
}
