package br.uepb.filasus.paciente;

import java.time.Instant;

/**
 * Resposta do cadastro feito pela recepção (US-05 - SCRUM-40).
 *
 * O pacienteId é o que a recepção usa para colocar o paciente na fila.
 * jaCadastrado diz se o paciente acabou de ser criado (false) ou se o CPF já
 * existia e este é o cadastro existente (true, CT-US05-02).
 * O CPF volta só mascarado ("529.***.***-25"), para a recepcionista conferir
 * (LGPD: só o dado necessário).
 */
public record CadastroPresencialResponse(
    Long pacienteId,
    String nome,
    String cpfMascarado,
    Prioridade prioridade,
    Instant cadastradoEm,
    boolean jaCadastrado,
    String versaoTermo,
    Instant consentimentoEm
) {
}
