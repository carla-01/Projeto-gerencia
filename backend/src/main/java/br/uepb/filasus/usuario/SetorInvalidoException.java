package br.uepb.filasus.usuario;

/** Setor do funcionário fora da regra da PO (US03): só o profissional tem setor, e ele é obrigatório. */
public class SetorInvalidoException extends RuntimeException {

    public SetorInvalidoException(String mensagem) {
        super(mensagem);
    }
}
