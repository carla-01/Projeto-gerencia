package br.uepb.filasus.fila;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo do POST /api/recepcao/check-in/busca (US-10 - SCRUM-154).
 *
 * "busca" é o CPF (com ou sem pontos e traço) ou o número da senha.
 * Vai no corpo, e não no endereço, para o CPF não aparecer nos registros do servidor.
 */
public record BuscaCheckInRequest(
    @NotBlank(message = "Informe o CPF ou o número da senha") String busca
) {

    /** O CPF é dado pessoal: não aparece em log. */
    @Override
    public String toString() {
        return "BuscaCheckInRequest[busca=oculta]";
    }
}
