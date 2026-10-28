package br.uepb.filasus.paciente;

/**
 * Lançada quando os dados do cadastro do paciente não passam nas regras
 * (ex.: CPF com dígito verificador errado). Vira 400 na API.
 *
 * Leva o nome do campo com problema, para a tela mostrar o erro no lugar certo.
 */
public class CadastroInvalidoException extends RuntimeException {

    private final String campo;

    public CadastroInvalidoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
