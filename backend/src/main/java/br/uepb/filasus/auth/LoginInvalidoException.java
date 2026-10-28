package br.uepb.filasus.auth;

/**
 * Lançada quando o login falha por qualquer motivo (usuário inexistente,
 * senha errada ou usuário inativo).
 *
 * A mensagem é sempre a mesma para não revelar se o CPF existe ou não.
 */
public class LoginInvalidoException extends RuntimeException {

    public static final String MENSAGEM = "CPF ou senha inválidos";

    public LoginInvalidoException() {
        super(MENSAGEM);
    }
}
