package br.uepb.filasus.paciente;

import java.time.Instant;

/**
 * Resposta do cadastro feito pela recepção (US-05 - SCRUM-40).
 *
 * O pacienteId é o que a recepção usa para colocar o paciente na fila.
 * Devolve também o registro do consentimento (versão e data e hora).
 * O CPF não volta na resposta (LGPD: só o dado necessário).
 */
public record CadastroPresencialResponse(
    Long pacienteId,
    String nome,
    Prioridade prioridade,
    String versaoTermo,
    Instant consentimentoEm
) {
}
