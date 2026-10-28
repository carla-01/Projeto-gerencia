package br.uepb.filasus.paciente;

import java.time.Instant;

/**
 * Resposta do autocadastro (US-04 - SCRUM-39).
 *
 * Devolve o registro do aceite (versão e data e hora) para a tela confirmar.
 * O CPF não volta na resposta (LGPD: só o dado necessário).
 */
public record AutocadastroResponse(
    Long pacienteId,
    String nome,
    Prioridade prioridade,
    String versaoTermo,
    Instant aceitoEm
) {
}
