package br.uepb.filasus.fila;

import java.time.Instant;

import br.uepb.filasus.paciente.Prioridade;

/** Senha depois de a recepção marcar a prioridade (US-11). */
public record PrioridadeSenhaResponse(Long id, int numero, String servico, StatusSenha status,
                                      Prioridade prioridade, Long prioridadePor, Instant prioridadeEm) {

    public PrioridadeSenhaResponse(Senha senha) {
        this(senha.getId(), senha.getNumero(), senha.getServico().getNome(), senha.getStatus(),
            senha.getPrioridadeAplicada(), senha.getPrioridadePor().getId(), senha.getPrioridadeEm());
    }
}
