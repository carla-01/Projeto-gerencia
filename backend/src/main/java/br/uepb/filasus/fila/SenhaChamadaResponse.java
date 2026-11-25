package br.uepb.filasus.fila;

import java.time.Instant;

import br.uepb.filasus.paciente.Prioridade;

/** Senha que o profissional acabou de chamar (US-13). */
public record SenhaChamadaResponse(Long id, int numero, String servico, String nomePaciente,
                                   Prioridade prioridade, StatusSenha status, Instant chamadaEm) {

    public SenhaChamadaResponse(Senha senha) {
        this(senha.getId(), senha.getNumero(), senha.getServico().getNome(), senha.getPaciente().getNome(),
            senha.getPrioridadeAplicada(), senha.getStatus(), senha.getChamadaEm());
    }
}
