package br.uepb.filasus.fila;

import java.time.Instant;

import br.uepb.filasus.paciente.Prioridade;

/** Senha mostrada à recepção no check-in (US-10). O CPF vem escondido: "529.***.***-25". */
public record SenhaCheckInResponse(Long id, int numero, String servico, String nomePaciente, String cpfMascarado,
                                   StatusSenha status, Prioridade prioridade, Instant checkinEm) {

    public SenhaCheckInResponse(Senha senha, String cpfMascarado) {
        this(senha.getId(), senha.getNumero(), senha.getServico().getNome(), senha.getPaciente().getNome(),
            cpfMascarado, senha.getStatus(), senha.getPrioridadeAplicada(), senha.getCheckinEm());
    }
}
