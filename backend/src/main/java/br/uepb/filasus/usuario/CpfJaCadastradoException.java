package br.uepb.filasus.usuario;

/** Já existe um usuário com o CPF informado (US03). */
public class CpfJaCadastradoException extends RuntimeException {

    public CpfJaCadastradoException() {
        super("CPF já cadastrado");
    }
}
