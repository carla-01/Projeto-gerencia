package br.uepb.filasus.atendimento;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uepb.filasus.fila.ProfissionalSemSetorException;
import br.uepb.filasus.fila.Senha;
import br.uepb.filasus.fila.SenhaChamadaResponse;
import br.uepb.filasus.fila.SenhaNaoEncontradaException;
import br.uepb.filasus.fila.SenhaRepository;
import br.uepb.filasus.fila.StatusSenha;
import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;

/**
 * Início e fim do atendimento pelo profissional de saúde (US-14 - SCRUM-167).
 *
 * Iniciar: a senha precisa estar CHAMADA. Cria o atendimento com iniciado_em e a senha
 * passa para EM_ATENDIMENTO.
 * Finalizar: a senha precisa estar EM_ATENDIMENTO. Grava finalizado_em e a senha passa
 * para ATENDIDA, saindo da fila (Cenário 1). Finalizar sem iniciar é recusado (Cenário 2).
 *
 * O profissional só mexe nas senhas dos seus setores; as outras são tratadas como inexistentes.
 */
@Service
public class AtendimentoService {

    static final String MENSAGEM_INICIE_ANTES = "Inicie o atendimento antes de finalizar";
    static final String MENSAGEM_JA_INICIADO = "O atendimento desta senha já foi iniciado";
    static final String MENSAGEM_JA_FINALIZADO = "O atendimento desta senha já foi finalizado";
    static final String MENSAGEM_NAO_CHAMADA = "Esta senha não foi chamada";
    static final String MENSAGEM_OUTRO_PROFISSIONAL = "Este atendimento foi iniciado por outro profissional";

    private static final Logger log = LoggerFactory.getLogger(AtendimentoService.class);

    private final UsuarioRepository usuarioRepository;
    private final SenhaRepository senhaRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final Clock clock;

    public AtendimentoService(UsuarioRepository usuarioRepository, SenhaRepository senhaRepository,
                              AtendimentoRepository atendimentoRepository, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.senhaRepository = senhaRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.clock = clock;
    }

    /** Inicia o atendimento da senha chamada. profissionalId vem do token. */
    @Transactional
    public AtendimentoResponse iniciar(Long profissionalId, Long senhaId) {
        Usuario profissional = buscarProfissional(profissionalId);
        Senha senha = buscarSenhaDoSetor(profissional, senhaId);

        switch (senha.getStatus()) {
            case CHAMADA -> { }
            case EM_ATENDIMENTO -> throw new AtendimentoNaoPermitidoException(MENSAGEM_JA_INICIADO);
            case ATENDIDA -> throw new AtendimentoNaoPermitidoException(MENSAGEM_JA_FINALIZADO);
            default -> throw new AtendimentoNaoPermitidoException(MENSAGEM_NAO_CHAMADA);
        }

        // Só muda se a senha ainda estiver CHAMADA: se dois cliques chegarem juntos,
        // o segundo não abre outro atendimento.
        if (senhaRepository.iniciarAtendimentoSeChamada(senhaId) == 0) {
            throw new AtendimentoNaoPermitidoException(MENSAGEM_JA_INICIADO);
        }
        Senha emAtendimento = senhaRepository.findById(senhaId).orElseThrow(SenhaNaoEncontradaException::new);

        Atendimento atendimento = atendimentoRepository.save(
            new Atendimento(emAtendimento, profissional, Instant.now(clock)));
        log.info("Atendimento id={} da senha id={} iniciado pelo profissional id={}",
            atendimento.getId(), senhaId, profissionalId);
        return new AtendimentoResponse(atendimento);
    }

    /** Finaliza o atendimento iniciado. profissionalId vem do token. */
    @Transactional
    public AtendimentoResponse finalizar(Long profissionalId, Long senhaId) {
        Usuario profissional = buscarProfissional(profissionalId);
        Senha senha = buscarSenhaDoSetor(profissional, senhaId);

        switch (senha.getStatus()) {
            case EM_ATENDIMENTO -> { }
            case CHAMADA -> throw new AtendimentoNaoPermitidoException(MENSAGEM_INICIE_ANTES);
            case ATENDIDA -> throw new AtendimentoNaoPermitidoException(MENSAGEM_JA_FINALIZADO);
            default -> throw new AtendimentoNaoPermitidoException(MENSAGEM_NAO_CHAMADA);
        }

        Atendimento atendimento = atendimentoRepository.findBySenha_Id(senhaId)
            .orElseThrow(() -> new AtendimentoNaoPermitidoException(MENSAGEM_INICIE_ANTES));
        if (!atendimento.getProfissional().getId().equals(profissionalId)) {
            throw new AtendimentoNaoPermitidoException(MENSAGEM_OUTRO_PROFISSIONAL);
        }

        // Com ATENDIDA a senha sai da fila e libera o paciente para tirar outra (STATUS_ATIVOS).
        senha.setStatus(StatusSenha.ATENDIDA);
        atendimento.setFinalizadoEm(Instant.now(clock));
        log.info("Atendimento id={} da senha id={} finalizado pelo profissional id={}",
            atendimento.getId(), senhaId, profissionalId);
        return new AtendimentoResponse(atendimento);
    }

    /**
     * Situação da senha e do atendimento dela, para a tela do profissional recuperar o paciente
     * chamado depois de recarregar a página (SCRUM-168). Só vale para senhas dos setores dele.
     */
    @Transactional(readOnly = true)
    public SenhaEmAndamentoResponse consultar(Long profissionalId, Long senhaId) {
        Usuario profissional = buscarProfissional(profissionalId);
        Senha senha = buscarSenhaDoSetor(profissional, senhaId);
        AtendimentoResponse atendimento = atendimentoRepository.findBySenha_Id(senhaId)
            .map(AtendimentoResponse::new)
            .orElse(null);
        return new SenhaEmAndamentoResponse(new SenhaChamadaResponse(senha), atendimento);
    }

    private Usuario buscarProfissional(Long profissionalId) {
        Usuario profissional = usuarioRepository.findById(profissionalId)
            .orElseThrow(ProfissionalSemSetorException::new);
        if (profissional.getSetores().isEmpty()) {
            throw new ProfissionalSemSetorException();
        }
        return profissional;
    }

    /** Senha de um dos setores do profissional. Senha de outro setor responde como inexistente. */
    private Senha buscarSenhaDoSetor(Usuario profissional, Long senhaId) {
        Senha senha = senhaRepository.findById(senhaId).orElseThrow(SenhaNaoEncontradaException::new);
        Long servicoId = senha.getServico().getId();
        boolean doSetor = profissional.getSetores().stream().anyMatch(setor -> setor.getId().equals(servicoId));
        if (!doSetor) {
            throw new SenhaNaoEncontradaException();
        }
        return senha;
    }
}
