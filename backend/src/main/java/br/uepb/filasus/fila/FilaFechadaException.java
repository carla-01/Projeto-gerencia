package br.uepb.filasus.fila;

/** Fora do horário do serviço ou com o limite de senhas do dia atingido (US-06 e US-09). */
public class FilaFechadaException extends RuntimeException {

    public FilaFechadaException() {
        super("Fila fechada");
    }
}
