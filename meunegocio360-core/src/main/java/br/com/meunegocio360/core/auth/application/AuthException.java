package br.com.meunegocio360.core.auth.application;

public class AuthException extends RuntimeException {
  private final String code;
  private final int status;
  private final long retryAfter;
  public AuthException(String code, int status) { this(code, status, 0); }
  public AuthException(String code, int status, long retryAfter) {
    super(switch (code) {
      case "INVALID_CREDENTIALS" -> "E-mail ou senha inválidos.";
      case "ACCOUNT_TEMPORARILY_LOCKED" -> "Conta temporariamente bloqueada.";
      case "INVALID_REFRESH_TOKEN" -> "Sessão inválida ou expirada.";
      case "ORIGIN_NOT_ALLOWED" -> "Origem não permitida.";
      default -> "Não foi possível processar a solicitação.";
    });
    this.code = code; this.status = status; this.retryAfter = retryAfter;
  }
  public String getCode() { return code; }
  public int getStatus() { return status; }
  public long getRetryAfter() { return retryAfter; }
}
