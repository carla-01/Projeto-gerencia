package br.uepb.filasus.atendimento;

import java.time.Instant;

import br.uepb.filasus.fila.Senha;
import br.uepb.filasus.fila.StatusSenha;

/** Atendimento depois de iniciar ou finalizar (US-14). finalizadoEm vem nulo enquanto não termina. */
public record AtendimentoResponse(Long id, Long senhaId, int numero, String servico, String nomePaciente,
                                  StatusSenha status, Instant iniciadoEm, Instant finalizadoEm) {

    public AtendimentoResponse(Atendimento atendimento) {
        this(atendimento, atendimento.getSenha());
    }

    private AtendimentoResponse(Atendimento atendimento, Senha senha) {
        this(atendimento.getId(), senha.getId(), senha.getNumero(), senha.getServico().getNome(),
            senha.getPaciente().getNome(), senha.getStatus(), atendimento.getIniciadoEm(),
            atendimento.getFinalizadoEm());
    }
}
