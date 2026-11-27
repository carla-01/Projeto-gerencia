package br.uepb.filasus.fila;

/** A prioridade não pode ser salva com os dados informados (US-11). Guarda o campo para a tela marcar. */
public class PrioridadeInvalidaException extends RuntimeException {

    private final String campo;

    public PrioridadeInvalidaException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
