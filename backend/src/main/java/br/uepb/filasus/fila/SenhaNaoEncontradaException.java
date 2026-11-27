package br.uepb.filasus.fila;

/** A senha não existe ou não é do paciente logado (US-08). */
public class SenhaNaoEncontradaException extends RuntimeException {

    public SenhaNaoEncontradaException() {
        super("Senha não encontrada");
    }
}
