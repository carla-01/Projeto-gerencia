package br.uepb.filasus.fila;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ServicoResponse(
    Long id,
    String nome,
    @JsonFormat(pattern = "HH:mm") LocalTime horarioAbertura,
    @JsonFormat(pattern = "HH:mm") LocalTime horarioFechamento) {

    public ServicoResponse(Servico servico, ConfiguracaoServico configuracao) {
        this(servico.getId(), servico.getNome(),
            configuracao.getHoraAbertura(), configuracao.getHoraFechamento());
    }
}
