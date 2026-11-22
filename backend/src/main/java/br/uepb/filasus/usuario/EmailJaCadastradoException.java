package br.uepb.filasus.usuario;

/** Cenário 2 da US03: já existe um usuário com o e-mail informado. */
public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException() {
        super("E-mail já cadastrado");
    }
}
