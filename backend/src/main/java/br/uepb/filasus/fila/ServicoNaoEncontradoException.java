package br.uepb.filasus.fila;

/** Serviço inexistente ou desativado pelo gestor (US-06). */
public class ServicoNaoEncontradoException extends RuntimeException {

    public ServicoNaoEncontradoException() {
        super("Serviço não encontrado");
    }
}
