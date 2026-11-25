package br.uepb.filasus.fila;

/** O texto da busca do check-in não é um CPF válido nem um número de senha (US-10). */
public class BuscaInvalidaException extends RuntimeException {

    public BuscaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
