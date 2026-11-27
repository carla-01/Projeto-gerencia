package br.uepb.filasus.fila;

/** O paciente já tem uma senha AGUARDANDO ou PRESENTE neste serviço hoje (US-06). */
public class SenhaAtivaExistenteException extends RuntimeException {

    public SenhaAtivaExistenteException() {
        super("Você já tem uma senha ativa neste serviço");
    }
}
