package br.uepb.filasus.fila;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uepb.filasus.paciente.Paciente;
import br.uepb.filasus.paciente.PacienteRepository;

/**
 * Posição do paciente na fila (US-07 - SCRUM-139).
 *
 * Na frente ficam as senhas do mesmo serviço e do mesmo dia que ainda estão na fila
 * (AGUARDANDO ou PRESENTE) e têm número menor. Senha chamada, em atendimento, atendida,
 * cancelada ou faltosa não conta. A prioridade legal muda a ordem quando entrar a US-11.
 */
@Service
public class PosicaoFilaService {

    private final PacienteRepository pacienteRepository;
    private final SenhaRepository senhaRepository;
    private final Clock clock;

    public PosicaoFilaService(PacienteRepository pacienteRepository, SenhaRepository senhaRepository,
                              Clock clock) {
        this.pacienteRepository = pacienteRepository;
        this.senhaRepository = senhaRepository;
        this.clock = clock;
    }

    /**
     * Senhas ativas de hoje do paciente logado (usuarioId vem do token), com a posição de cada uma.
     * Lista vazia quando ele não está em nenhuma fila.
     */
    @Transactional(readOnly = true)
    public List<PosicaoFilaResponse> minhasSenhas(Long usuarioId) {
        Paciente paciente = pacienteRepository.findByUsuario_Id(usuarioId)
            .orElseThrow(PacienteNaoEncontradoException::new);

        // Dia local da UBS: o Clock do projeto está no fuso da unidade (RelogioConfig).
        LocalDate hoje = LocalDate.now(clock);
        return senhaRepository.buscarSenhasAtivas(paciente, hoje).stream()
            .map(senha -> new PosicaoFilaResponse(senha,
                (int) senhaRepository.contarAFrente(senha.getServico(), hoje, senha.getNumero())))
            .toList();
    }
}
