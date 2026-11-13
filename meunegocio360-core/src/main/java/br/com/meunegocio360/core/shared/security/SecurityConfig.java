package br.com.meunegocio360.core.shared.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
public class SecurityConfig {
  @Bean
  SecretKey jwtSecretKey(@Value("${app.auth.jwt.secret}") String secret) {
    byte[] bytes;
    try { bytes = java.util.Base64.getDecoder().decode(secret); }
    catch (IllegalArgumentException ex) { bytes = secret.getBytes(StandardCharsets.UTF_8); }
    if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET deve conter ao menos 32 bytes.");
    return new SecretKeySpec(bytes, "HmacSHA256");
  }
  @Bean
  JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
  @Bean
  JwtDecoder jwtDecoder(SecretKey key, @Value("${app.auth.jwt.issuer}") String issuer) {
    var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    return decoder;
  }
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment) throws Exception {
    boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
    http.csrf(AbstractHttpConfigurer::disable).cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> {
          auth.requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout", "/actuator/health").permitAll();
          if (!prod) auth.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
          auth.anyRequest().authenticated();
        })
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()).authenticationEntryPoint(new ProblemAuthenticationEntryPoint()));
    return http.build();
  }
  @Bean
  CorsConfigurationSource corsConfigurationSource(@Value("${app.auth.allowed-origins}") String origins) {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
    if (config.getAllowedOrigins().isEmpty() || config.getAllowedOrigins().contains("*"))
      throw new IllegalStateException("CORS_ALLOWED_ORIGINS deve conter origens explícitas e não pode usar *.");
    config.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS"));
    config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Request-Id"));
    config.setExposedHeaders(Arrays.asList("X-Request-Id", "Retry-After", "WWW-Authenticate"));
    config.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", config); return source;
  }
}
