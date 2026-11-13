package br.com.meunegocio360.core.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final ObjectMapper mapper = new ObjectMapper();
  @Override public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
    response.setStatus(401); response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setHeader("WWW-Authenticate", "Bearer");
    mapper.writeValue(response.getOutputStream(), Map.of("type", "about:blank", "title", "Não autenticado", "status", 401,
        "detail", "Autenticação necessária.", "instance", request.getRequestURI(), "code", "UNAUTHORIZED",
        "traceId", java.util.Objects.toString(MDC.get("requestId"), "")));
  }
}
