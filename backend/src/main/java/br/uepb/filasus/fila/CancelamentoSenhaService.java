package br.uepb.filasus.fila;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uepb.filasus.paciente.Paciente;
import br.uepb.filasus.paciente.PacienteRepository;

/**
 * Cancelamento da senha pelo próprio paciente (US-08 - SCRUM-43).
 *
 * A senha cancelada sai da fila: a posição dos outros pacientes é contada só com as
 * senhas AGUARDANDO ou PRESENTE, então quem estava atrás anda uma posição.
 */
@Service
public class CancelamentoSenhaService {

    static final String MENSAGEM_ATENDIMENTO_COMECOU = "Seu atendimento já começou";
    static final String MENSAGEM_FORA_DA_FILA = "Esta senha não está mais na fila";

    private final PacienteRepository pacienteRepository;
    private final SenhaRepository senhaRepository;

    public CancelamentoSenhaService(PacienteRepository pacienteRepository, SenhaRepository senhaRepository) {
        this.pacienteRepository = pacienteRepository;
        this.senhaRepository = senhaRepository;
    }

    /** Cancela a senha do paciente logado (usuarioId vem do token). */
    @Transactional
    public SenhaResponse cancelar(Long usuarioId, Long senhaId) {
        Optional<Paciente> pacienteEncontrado = pacienteRepository.findByUsuario_Id(usuarioId);
        if (pacienteEncontrado.isEmpty()) {
            throw new PacienteNaoEncontradoException();
        }
        Paciente paciente = pacienteEncontrado.get();

        Optional<Senha> senhaEncontrada = senhaRepository.findById(senhaId);
        if (senhaEncontrada.isEmpty()) {
            throw new SenhaNaoEncontradaException();
        }
        Senha senha = senhaEncontrada.get();

        // Só o dono cancela a senha. Para os outros pacientes ela é tratada como inexistente.
        if (!senha.getPaciente().getId().equals(paciente.getId())) {
            throw new SenhaNaoEncontradaException();
        }

        StatusSenha status = senha.getStatus();
        if (status == StatusSenha.CHAMADA || status == StatusSenha.EM_ATENDIMENTO) {
            throw new CancelamentoNaoPermitidoException(MENSAGEM_ATENDIMENTO_COMECOU);
        }
        if (status != StatusSenha.AGUARDANDO && status != StatusSenha.PRESENTE) {
            throw new CancelamentoNaoPermitidoException(MENSAGEM_FORA_DA_FILA);
        }

        senha.setStatus(StatusSenha.CANCELADA);
        senhaRepository.save(senha);
        return new SenhaResponse(senha);
    }
}
