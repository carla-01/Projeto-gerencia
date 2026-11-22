package br.uepb.filasus.usuario;

/** CPF do funcionário com dígitos verificadores errados (US03). */
public class CpfInvalidoException extends RuntimeException {

    public CpfInvalidoException() {
        super("CPF inválido");
    }
}
