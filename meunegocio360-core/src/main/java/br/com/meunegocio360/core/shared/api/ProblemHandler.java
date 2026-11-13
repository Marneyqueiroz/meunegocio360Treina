package br.com.meunegocio360.core.shared.api;

import br.com.meunegocio360.core.auth.application.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.beans.factory.annotation.Value;

@RestControllerAdvice
public class ProblemHandler {
  private final boolean cookieSecure;
  public ProblemHandler(@Value("${app.auth.cookie.secure}") boolean cookieSecure) { this.cookieSecure = cookieSecure; }
  @ExceptionHandler(AuthException.class)
  ResponseEntity<Map<String, Object>> auth(AuthException ex, HttpServletRequest request, HttpServletResponse response) {
    var headers = new HttpHeaders();
    if (ex.getStatus() == 429) headers.set("Retry-After", Long.toString(ex.getRetryAfter()));
    if ("UNAUTHORIZED".equals(ex.getCode())) headers.set("WWW-Authenticate", "Bearer");
    if ("INVALID_REFRESH_TOKEN".equals(ex.getCode())) {
      response.addHeader("Set-Cookie", org.springframework.http.ResponseCookie.from("mn360_refresh", "")
          .httpOnly(true).secure(cookieSecure).sameSite("Strict").path("/api/v1/auth").maxAge(0).build().toString());
    }
    return response(ex.getStatus(), ex.getCode(), ex.getMessage(), request, headers, List.of());
  }
  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  ResponseEntity<Map<String, Object>> validation(Exception ex, HttpServletRequest request) {
    List<Map<String, String>> errors = ex instanceof MethodArgumentNotValidException invalid
        ? invalid.getBindingResult().getFieldErrors().stream().map(this::fieldError).toList()
        : List.of(Map.of("field", "body", "message", "JSON inválido ou ausente."));
    return response(400, "VALIDATION_ERROR", "Um ou mais campos são inválidos.", request, new HttpHeaders(), errors);
  }
  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, Object>> internal(Exception ex, HttpServletRequest request) {
    return response(500, "INTERNAL_ERROR", "Erro inesperado.", request, new HttpHeaders(), List.of());
  }
  private Map<String, String> fieldError(FieldError error) {
    String field = "senha".equals(error.getField()) ? "senha" : "email".equals(error.getField()) ? "email" : error.getField();
    return Map.of("field", field, "message", "valor inválido");
  }
  private ResponseEntity<Map<String, Object>> response(int status, String code, String detail, HttpServletRequest request,
      HttpHeaders headers, List<?> errors) {
    var body = new java.util.LinkedHashMap<String, Object>();
    body.put("type", "https://meunegocio360.com.br/problemas/" + code.toLowerCase(java.util.Locale.ROOT));
    body.put("title", status == 400 ? "Dados inválidos" : status == 500 ? "Erro interno" : "Falha na solicitação");
    body.put("status", status); body.put("detail", detail); body.put("instance", request.getRequestURI());
    body.put("code", code); body.put("traceId", java.util.Objects.toString(MDC.get("requestId"), ""));
    if (!errors.isEmpty()) body.put("errors", errors);
    headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    return new ResponseEntity<>(body, headers, HttpStatus.valueOf(status));
  }
}
