package br.uepb.filasus.paciente;

/**
 * Lançada quando já existe cadastro com o mesmo CPF ou e-mail. Vira 409 na API.
 */
public class CadastroDuplicadoException extends RuntimeException {

    public CadastroDuplicadoException(String mensagem) {
        super(mensagem);
    }
}
