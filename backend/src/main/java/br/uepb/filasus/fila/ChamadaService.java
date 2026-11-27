package br.uepb.filasus.fila;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;

/**
 * Chamada do próximo paciente (US-13 - SCRUM-162).
 *
 * Só entram na chamada as senhas de hoje com check-in feito (PRESENTE) nos serviços do
 * profissional. A ordem é a da prioridade legal (OrdemFila, US-11). A senha escolhida passa
 * para CHAMADA e guarda o horário (chamada_em).
 */
@Service
public class ChamadaService {

    static final String MENSAGEM_FILA_VAZIA = "Não há pacientes aguardando";

    private static final Logger log = LoggerFactory.getLogger(ChamadaService.class);

    private final UsuarioRepository usuarioRepository;
    private final SenhaRepository senhaRepository;
    private final Clock clock;

    public ChamadaService(UsuarioRepository usuarioRepository, SenhaRepository senhaRepository, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.senhaRepository = senhaRepository;
        this.clock = clock;
    }

    /** Chama o próximo paciente presente. profissionalId vem do token. */
    @Transactional
    public ChamadaResponse chamarProximo(Long profissionalId) {
        Usuario profissional = usuarioRepository.findById(profissionalId)
            .orElseThrow(ProfissionalSemSetorException::new);
        Set<Servico> setores = profissional.getSetores();
        if (setores.isEmpty()) {
            throw new ProfissionalSemSetorException();
        }

        for (Senha candidata : presentesEmOrdem(setores)) {
            // Só muda se a senha ainda estiver PRESENTE. Se outro profissional chamou a mesma
            // pessoa um instante antes, nada muda aqui e passamos para a próxima.
            int alteradas = senhaRepository.chamarSePresente(candidata.getId(), Instant.now(clock));
            if (alteradas == 1) {
                Senha chamada = senhaRepository.findById(candidata.getId())
                    .orElseThrow(SenhaNaoEncontradaException::new);
                log.info("Senha id={} chamada pelo profissional id={}", chamada.getId(), profissionalId);
                return new ChamadaResponse(null, new SenhaChamadaResponse(chamada));
            }
        }
        return new ChamadaResponse(MENSAGEM_FILA_VAZIA, null);
    }

    /** Senhas de hoje com check-in nos serviços do profissional, na ordem de atendimento. */
    private List<Senha> presentesEmOrdem(Set<Servico> setores) {
        LocalDate hoje = LocalDate.now(clock);
        List<Senha> presentes = new ArrayList<>();
        for (Servico setor : setores) {
            presentes.addAll(senhaRepository.findByServicoAndDataRefAndStatusIn(
                setor, hoje, List.of(StatusSenha.PRESENTE)));
        }
        presentes.sort(OrdemFila.ORDEM);
        return presentes;
    }
}
