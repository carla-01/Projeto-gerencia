package br.uepb.filasus.atendimento;

import br.uepb.filasus.fila.SenhaChamadaResponse;

/**
 * Senha que o profissional chamou e a situação do atendimento dela (US14 - SCRUM-168).
 * Usada para a tela recuperar o paciente depois de recarregar. "atendimento" vem nulo
 * enquanto o atendimento não foi iniciado.
 */
public record SenhaEmAndamentoResponse(SenhaChamadaResponse senha, AtendimentoResponse atendimento) {
}
