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
 * (AGUARDANDO ou PRESENTE) e vêm antes na ordem de atendimento (OrdemFila): a prioridade
 * legal passa na frente e, no mesmo nível, vale o número da senha (US-11 - SCRUM-159).
 * Senha chamada, em atendimento, atendida, cancelada ou faltosa não conta.
 * A senha chamada ou em atendimento continua aparecendo para o dono, sem posição
 * (posição 0), para ele ver que foi chamado (US-13 - SCRUM-163).
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
            .map(senha -> montarResposta(senha, hoje))
            .toList();
    }

    private PosicaoFilaResponse montarResposta(Senha senha, LocalDate hoje) {
        if (!SenhaRepository.STATUS_NA_FILA.contains(senha.getStatus())) {
            return PosicaoFilaResponse.foraDaFila(senha);
        }
        return new PosicaoFilaResponse(senha, pessoasAFrente(senha, hoje));
    }

    /** Quantas senhas vêm antes desta na fila do serviço, já com a prioridade legal. */
    private int pessoasAFrente(Senha minha, LocalDate hoje) {
        int aFrente = 0;
        for (Senha senha : senhaRepository.filaDoDia(minha.getServico(), hoje)) {
            if (senha.getId().equals(minha.getId())) {
                break;
            }
            aFrente = aFrente + 1;
        }
        return aFrente;
    }
}
