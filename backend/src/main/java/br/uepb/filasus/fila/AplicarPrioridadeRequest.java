package br.uepb.filasus.fila;

import br.uepb.filasus.paciente.Prioridade;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo do PUT /api/recepcao/senhas/{senhaId}/prioridade (US-11 - SCRUM-158).
 *
 * O motivo é obrigatório só na urgência (conferido no PrioridadeSenhaService).
 * Exemplo: { "prioridade": "URGENCIA", "motivo": "Dor no peito" }
 */
public record AplicarPrioridadeRequest(
    @NotNull(message = "Escolha a prioridade") Prioridade prioridade,
    @Size(max = 255, message = "O motivo deve ter no máximo 255 caracteres") String motivo
) {

    /** O motivo pode ter informação de saúde: não aparece em log. */
    @Override
    public String toString() {
        return "AplicarPrioridadeRequest[prioridade=" + prioridade + "]";
    }
}
