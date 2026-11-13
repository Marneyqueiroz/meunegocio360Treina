package br.com.meunegocio360.core.auth.api;

import br.com.meunegocio360.core.auth.application.AuthException;
import br.com.meunegocio360.core.auth.application.AuthService;
import br.com.meunegocio360.core.auth.application.AuthService.SessaoCriada;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private static final String COOKIE = "mn360_refresh";
  private final AuthService authService;
  private final boolean cookieSecure;
  private final String allowedOrigins;
  public AuthController(AuthService authService,
      @Value("${app.auth.cookie.secure}") boolean cookieSecure,
      @Value("${app.auth.allowed-origins}") String allowedOrigins) {
    this.authService = authService; this.cookieSecure = cookieSecure; this.allowedOrigins = allowedOrigins;
  }

  @PostMapping("/login")
  public ResponseEntity<SessaoResponse> login(@Valid @RequestBody LoginRequest request) {
    SessaoCriada result = authService.login(request);
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(result.refreshToken(), result.refreshExpiraEm()).toString()).body(result.response());
  }
  @PostMapping("/refresh")
  public ResponseEntity<SessaoResponse> refresh(HttpServletRequest request) {
    validarOrigin(request);
    SessaoCriada result = authService.renovar(cookieValue(request));
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(result.refreshToken(), result.refreshExpiraEm()).toString()).body(result.response());
  }
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    validarOrigin(request);
    authService.logout(cookieValue(request));
    return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", java.time.Instant.EPOCH).toString()).build();
  }
  @GetMapping("/me")
  public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
    return authService.usuarioAutenticado(UUID.fromString(jwt.getSubject()));
  }
  private ResponseCookie cookie(String token, java.time.Instant expires) {
    long maxAge = Math.max(0, java.time.Duration.between(java.time.Instant.now(), expires).toSeconds());
    return ResponseCookie.from(COOKIE, token).httpOnly(true).secure(cookieSecure).sameSite("Strict")
        .path("/api/v1/auth").maxAge(maxAge).build();
  }
  private String cookieValue(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return null;
    return Arrays.stream(cookies).filter(c -> COOKIE.equals(c.getName())).map(Cookie::getValue).findFirst().orElse(null);
  }
  private void validarOrigin(HttpServletRequest request) {
    String origin = request.getHeader("Origin");
    if (origin != null && Arrays.stream(allowedOrigins.split(",")).map(String::trim).noneMatch(origin::equals)) {
      throw new AuthException("ORIGIN_NOT_ALLOWED", 403);
    }
  }
}
