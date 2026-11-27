package br.uepb.filasus.fila;

import jakarta.validation.constraints.NotNull;

/** Serviço escolhido pelo paciente para entrar na fila (US-06). */
public record EntradaFilaRequest(
    @NotNull(message = "Serviço é obrigatório")
    Long servicoId
) {
}
