package br.uepb.filasus.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo do POST /auth/login (US01 - SCRUM-37).
 *
 * O campo "login" aceita o CPF ou o e-mail do usuário.
 * Exemplo: { "login": "12345678901", "senha": "senha123" }
 */
public record LoginRequest(
    @NotBlank String login,
    @NotBlank String senha
) {
}
