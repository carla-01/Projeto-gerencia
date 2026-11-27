package br.uepb.filasus.fila;

import java.util.List;

/**
 * Resultado da busca do check-in (US-10).
 *
 * Quando há senha para confirmar, "mensagem" vem nula e "senhas" tem uma ou mais senhas.
 * Quando não há, "senhas" vem vazia e "mensagem" explica o motivo. Se o CPF é de um
 * paciente cadastrado, "pacienteId" vem preenchido para a recepção colocá-lo na fila.
 */
public record BuscaCheckInResponse(String mensagem, Long pacienteId, String nomePaciente,
                                   List<SenhaCheckInResponse> senhas) {
}
