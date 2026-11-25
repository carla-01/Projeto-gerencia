package br.uepb.filasus.fila;

/** A presença não pode ser confirmada: já foi confirmada ou a senha saiu da fila (US-10). */
public class CheckInNaoPermitidoException extends RuntimeException {

    public CheckInNaoPermitidoException(String mensagem) {
        super(mensagem);
    }
}
