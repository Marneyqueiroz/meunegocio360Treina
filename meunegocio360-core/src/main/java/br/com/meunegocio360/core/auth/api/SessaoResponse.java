package br.com.meunegocio360.core.auth.api;

public record SessaoResponse(String tokenType, String accessToken, long expiresIn, UsuarioResponse usuario) {}
