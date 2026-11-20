package br.uepb.filasus.usuario;

/** O gestor só cadastra RECEPCIONISTA e PROFISSIONAL (regra da PO, US03). */
public class PerfilNaoPermitidoException extends RuntimeException {

    public PerfilNaoPermitidoException() {
        super("Papel inválido: escolha RECEPCIONISTA ou PROFISSIONAL");
    }
}
