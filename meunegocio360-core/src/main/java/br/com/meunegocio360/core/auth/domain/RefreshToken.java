package br.com.meunegocio360.core.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(UUID usuarioId, UUID familiaId, Instant expiraEm, Instant revogadoEm) {}
