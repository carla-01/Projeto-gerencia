package br.uepb.filasus.fila;

/** A senha já foi chamada, atendida, cancelada ou deu falta: a prioridade não muda mais (US-11). */
public class SenhaForaDaFilaException extends RuntimeException {

    public SenhaForaDaFilaException() {
        super("Esta senha não está mais na fila");
    }
}
