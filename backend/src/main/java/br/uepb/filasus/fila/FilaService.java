package br.uepb.filasus.fila;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FilaService {

    private final ConfiguracaoServicoRepository configuracaoServicoRepository;
    private final SenhaRepository senhaRepository;
    private final Clock clock;

    public FilaService(ConfiguracaoServicoRepository configuracaoServicoRepository,
                       SenhaRepository senhaRepository, Clock clock) {
        this.configuracaoServicoRepository = configuracaoServicoRepository;
        this.senhaRepository = senhaRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public boolean estaAberta(Long servicoId) {
        ConfiguracaoServico configuracao = configuracaoServicoRepository.findByServico_Id(servicoId)
            .orElseThrow(ConfiguracaoServicoAusenteException::new);

        LocalDateTime agora = LocalDateTime.now(clock);
        LocalTime horarioAtual = agora.toLocalTime();
        if (horarioAtual.isBefore(configuracao.getHoraAbertura())
                || !horarioAtual.isBefore(configuracao.getHoraFechamento())) {
            return false;
        }

        long senhasEmitidasHoje = senhaRepository.countByServicoAndDataRef(
            configuracao.getServico(), agora.toLocalDate());
        return senhasEmitidasHoje < configuracao.getLimiteSenhasDia();
    }
}
