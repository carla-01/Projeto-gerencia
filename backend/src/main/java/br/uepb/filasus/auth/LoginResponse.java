package br.uepb.filasus.auth;

import br.uepb.filasus.usuario.Perfil;

/**
 * Resposta do login com sucesso (US01 - SCRUM-37).
 *
 * O front-end guarda o token e usa o perfil para abrir a tela inicial certa.
 */
public record LoginResponse(
    String token,
    Perfil perfil
) {
}
