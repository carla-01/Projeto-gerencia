package br.uepb.filasus.fila;

public class HorarioInvalidoException extends RuntimeException {

    public HorarioInvalidoException() {
        super("Horário inválido");
    }
}
