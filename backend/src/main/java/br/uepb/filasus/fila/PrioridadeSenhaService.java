package br.uepb.filasus.fila;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uepb.filasus.paciente.Prioridade;
import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;

/**
 * Prioridade legal marcada pela recepção (US-11 - SCRUM-158).
 *
 * A recepcionista marca IDOSO, GESTANTE, PCD ou URGENCIA numa senha que ainda está na fila
 * (AGUARDANDO ou PRESENTE). A senha guarda quem marcou, quando e o motivo (Cenário 1).
 * Urgência sem motivo é recusada (Cenário 2). A nova ordem da fila entra na SCRUM-159.
 */
@Service
public class PrioridadeSenhaService {

    static final String MENSAGEM_MOTIVO_OBRIGATORIO = "Informe o motivo da urgência";
    static final String MENSAGEM_PRIORIDADE_INVALIDA = "Escolha IDOSO, GESTANTE, PCD ou URGENCIA";

    /** Prioridades que a recepção marca. NENHUMA é o padrão da senha, não uma marcação. */
    static final Set<Prioridade> PRIORIDADES_PERMITIDAS =
        Set.of(Prioridade.IDOSO, Prioridade.GESTANTE, Prioridade.PCD, Prioridade.URGENCIA);

    private static final Logger log = LoggerFactory.getLogger(PrioridadeSenhaService.class);

    private final SenhaRepository senhaRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public PrioridadeSenhaService(SenhaRepository senhaRepository, UsuarioRepository usuarioRepository,
                                  Clock clock) {
        this.senhaRepository = senhaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    /** Marca a prioridade na senha. recepcionistaId vem do token. */
    @Transactional
    public PrioridadeSenhaResponse aplicar(Long senhaId, AplicarPrioridadeRequest request, Long recepcionistaId) {
        Prioridade prioridade = request.prioridade();
        if (!PRIORIDADES_PERMITIDAS.contains(prioridade)) {
            throw new PrioridadeInvalidaException("prioridade", MENSAGEM_PRIORIDADE_INVALIDA);
        }

        String motivo = request.motivo() == null ? null : request.motivo().trim();
        if (motivo != null && motivo.isEmpty()) {
            motivo = null;
        }
        if (prioridade == Prioridade.URGENCIA && motivo == null) {
            throw new PrioridadeInvalidaException("motivo", MENSAGEM_MOTIVO_OBRIGATORIO);
        }

        Senha senha = senhaRepository.findById(senhaId).orElseThrow(SenhaNaoEncontradaException::new);
        if (!SenhaRepository.STATUS_NA_FILA.contains(senha.getStatus())) {
            throw new SenhaForaDaFilaException();
        }

        Prioridade anterior = senha.getPrioridadeAplicada();
        Usuario recepcionista = usuarioRepository.getReferenceById(recepcionistaId);
        senha.aplicarPrioridade(prioridade, recepcionista, motivo, Instant.now(clock));
        senhaRepository.save(senha);

        // Só ids e prioridades no log: o motivo pode ter informação de saúde.
        log.info("Prioridade da senha id={} mudou de {} para {} pela recepcionista id={}",
            senha.getId(), anterior, prioridade, recepcionistaId);
        return new PrioridadeSenhaResponse(senha);
    }
}
