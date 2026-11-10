package br.uepb.filasus.fila;

import java.time.LocalDate;

/** Senha do paciente com a posição na fila e quantas pessoas estão na frente (US-07). */
public record PosicaoFilaResponse(Long id, int numero, String servico, LocalDate data, StatusSenha status,
                                  int posicao, int pessoasAFrente) {

    public PosicaoFilaResponse(Senha senha, int pessoasAFrente) {
        this(senha.getId(), senha.getNumero(), senha.getServico().getNome(), senha.getDataRef(),
            senha.getStatus(), pessoasAFrente + 1, pessoasAFrente);
    }
}
