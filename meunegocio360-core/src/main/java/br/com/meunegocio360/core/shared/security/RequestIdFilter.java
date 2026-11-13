package br.com.meunegocio360.core.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestIdFilter extends OncePerRequestFilter {
  @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String candidate = request.getHeader("X-Request-Id");
    String id;
    try { id = candidate != null && candidate.length() <= 100 ? UUID.fromString(candidate).toString() : UUID.randomUUID().toString(); }
    catch (IllegalArgumentException ex) { id = UUID.randomUUID().toString(); }
    MDC.put("requestId", id); response.setHeader("X-Request-Id", id);
    try { chain.doFilter(request, response); } finally { MDC.remove("requestId"); }
  }
}
