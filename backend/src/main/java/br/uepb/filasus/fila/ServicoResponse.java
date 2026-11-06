package br.uepb.filasus.fila;

import java.time.LocalTime;

public record ServicoResponse(Long id, String nome, LocalTime horarioAbertura, LocalTime horarioFechamento) {

    public ServicoResponse(Servico servico, ConfiguracaoServico configuracao) {
        this(servico.getId(), servico.getNome(),
            configuracao.getHoraAbertura(), configuracao.getHoraFechamento());
    }
}
