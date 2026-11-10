package br.uepb.filasus.fila;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.uepb.filasus.paciente.Paciente;
import br.uepb.filasus.paciente.PacienteRepository;
import br.uepb.filasus.paciente.Prioridade;

/**
 * Entrada do paciente na fila de um serviço (US-06 - SCRUM-136).
 *
 * O número da senha é o maior do dia + 1. Se dois pacientes entrarem ao mesmo tempo e
 * gerarem o mesmo número, a UNIQUE (servico_id, data_ref, numero) recusa o segundo e
 * a entrada é tentada de novo, numa transação nova, com o próximo número (DA-DES-I2-01).
 */
@Service
public class EntradaFilaService {

    /** Quantas vezes tenta gerar a senha quando outro paciente pega o mesmo número. */
    static final int TENTATIVAS = 3;

    private final PacienteRepository pacienteRepository;
    private final ServicoRepository servicoRepository;
    private final SenhaRepository senhaRepository;
    private final FilaService filaService;
    private final Clock clock;
    private final TransactionTemplate transacao;

    public EntradaFilaService(PacienteRepository pacienteRepository, ServicoRepository servicoRepository,
                              SenhaRepository senhaRepository, FilaService filaService, Clock clock,
                              PlatformTransactionManager transactionManager) {
        this.pacienteRepository = pacienteRepository;
        this.servicoRepository = servicoRepository;
        this.senhaRepository = senhaRepository;
        this.filaService = filaService;
        this.clock = clock;
        this.transacao = new TransactionTemplate(transactionManager);
    }

    /** Serviços ativos para o paciente escolher, dizendo se a fila de cada um está aberta agora. */
    public List<ServicoDisponivelResponse> servicosDisponiveis() {
        return servicoRepository.findByAtivoTrueOrderByNomeAsc().stream()
            .map(servico -> new ServicoDisponivelResponse(servico.getId(), servico.getNome(), filaAberta(servico)))
            .toList();
    }

    /** Gera a senha do paciente logado (usuarioId vem do token) na fila do serviço escolhido. */
    public SenhaResponse entrar(Long usuarioId, Long servicoId) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                return transacao.execute(status -> gerarSenha(usuarioId, servicoId));
            } catch (DataIntegrityViolationException numeroRepetido) {
                if (tentativa == TENTATIVAS) {
                    throw numeroRepetido;
                }
            }
        }
    }

    private SenhaResponse gerarSenha(Long usuarioId, Long servicoId) {
        Paciente paciente = pacienteRepository.findByUsuario_Id(usuarioId)
            .orElseThrow(PacienteNaoEncontradoException::new);
        Servico servico = servicoRepository.findById(servicoId)
            .filter(Servico::isAtivo)
            .orElseThrow(ServicoNaoEncontradoException::new);

        if (!filaAberta(servico)) {
            throw new FilaFechadaException();
        }

        // Dia local da UBS: o Clock do projeto está no fuso da unidade (RelogioConfig).
        LocalDate hoje = LocalDate.now(clock);
        if (senhaRepository.buscarSenhaAtiva(paciente, servico, hoje).isPresent()) {
            throw new SenhaAtivaExistenteException();
        }

        int numero = senhaRepository.maiorNumeroDoDia(servico, hoje) + 1;
        // A prioridade legal é aplicada pela recepção no check-in (US-11).
        Senha senha = senhaRepository.saveAndFlush(
            new Senha(servico, paciente, numero, hoje, Prioridade.NENHUMA, Instant.now(clock)));
        return new SenhaResponse(senha);
    }

    /** Serviço sem horário e limite configurados (US-09) não recebe senha. */
    private boolean filaAberta(Servico servico) {
        try {
            return filaService.estaAberta(servico.getId());
        } catch (ConfiguracaoServicoAusenteException semConfiguracao) {
            return false;
        }
    }
}
