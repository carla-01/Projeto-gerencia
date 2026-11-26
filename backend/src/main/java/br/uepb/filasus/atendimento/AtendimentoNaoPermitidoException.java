package br.uepb.filasus.atendimento;

/** A senha não está na situação certa para iniciar ou finalizar o atendimento (US-14). */
public class AtendimentoNaoPermitidoException extends RuntimeException {

    public AtendimentoNaoPermitidoException(String mensagem) {
        super(mensagem);
    }
}
