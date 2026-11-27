package br.uepb.filasus.fila;

/** A senha não pode mais ser cancelada pelo paciente (US-08). */
public class CancelamentoNaoPermitidoException extends RuntimeException {

    public CancelamentoNaoPermitidoException(String mensagem) {
        super(mensagem);
    }
}
