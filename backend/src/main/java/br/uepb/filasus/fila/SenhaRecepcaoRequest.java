package br.uepb.filasus.fila;

import jakarta.validation.constraints.NotNull;

/** Corpo do POST /api/recepcao/senhas (US-10 - SCRUM-155). */
public record SenhaRecepcaoRequest(
    @NotNull(message = "Escolha o paciente") Long pacienteId,
    @NotNull(message = "Escolha o serviço") Long servicoId
) {
}
