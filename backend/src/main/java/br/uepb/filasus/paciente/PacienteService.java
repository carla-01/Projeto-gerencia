package br.uepb.filasus.paciente;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uepb.filasus.lgpd.Cpf;
import br.uepb.filasus.lgpd.ProtecaoCpf;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;

/**
 * Regras do cadastro de paciente (US-04 - SCRUM-39).
 */
@Service
public class PacienteService {

    /** Mostrada quando o paciente tenta se cadastrar sem aceitar o termo (CT-US04-02). */
    public static final String FINALIDADE_DOS_DADOS =
        "Para se cadastrar, aceite o termo. Seus dados são usados só para organizar "
        + "a fila de atendimento da UBS, e você pode pedir a exclusão deles quando quiser.";

    /** Mostrada quando a recepção tenta cadastrar sem registrar o consentimento verbal (US-05). */
    public static final String CONSENTIMENTO_VERBAL_OBRIGATORIO =
        "Registre o consentimento verbal do paciente antes de cadastrar.";

    private static final Logger log = LoggerFactory.getLogger(PacienteService.class);

    private final PacienteRepository pacienteRepository;
    private final ConsentimentoRepository consentimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProtecaoCpf protecaoCpf;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String versaoTermo;

    public PacienteService(PacienteRepository pacienteRepository,
                           ConsentimentoRepository consentimentoRepository,
                           UsuarioRepository usuarioRepository,
                           ProtecaoCpf protecaoCpf,
                           PasswordEncoder passwordEncoder,
                           Clock clock,
                           @Value("${lgpd.versao-termo}") String versaoTermo) {
        this.pacienteRepository = pacienteRepository;
        this.consentimentoRepository = consentimentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.protecaoCpf = protecaoCpf;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.versaoTermo = versaoTermo;
    }

    /**
     * Autocadastro do paciente pelo celular (US-04).
     *
     * Cria o login (perfil PACIENTE), o paciente e o aceite do termo LGPD juntos:
     * se um falhar, nenhum fica salvo.
     */
    @Transactional
    public AutocadastroResponse autocadastrar(AutocadastroRequest request) {
        // O @AssertTrue do request já barra isso; a regra fica aqui também
        // para ninguém conseguir cadastrar sem aceite chamando o serviço direto.
        if (!request.aceiteTermo()) {
            throw new CadastroInvalidoException("aceiteTermo", FINALIDADE_DOS_DADOS);
        }

        String cpf = Cpf.normalizar(request.cpf());
        if (!Cpf.ehValido(cpf)) {
            throw new CadastroInvalidoException("cpf", "CPF inválido");
        }

        String cpfHash = protecaoCpf.hash(cpf);
        if (usuarioRepository.findByCpfHash(cpfHash).isPresent() || pacienteRepository.findByCpfHash(cpfHash).isPresent()) {
            throw new CadastroDuplicadoException("Já existe um cadastro com este CPF");
        }

        String email = request.email().trim();
        if (usuarioRepository.existsByEmail(email)) {
            throw new CadastroDuplicadoException("Já existe um cadastro com este e-mail");
        }

        Instant agora = Instant.now(clock);
        String nome = request.nome().trim();
        String cpfCifrado = protecaoCpf.cifrar(cpf);
        Prioridade prioridade = request.prioridade() != null ? request.prioridade() : Prioridade.NENHUMA;

        Usuario usuario = usuarioRepository.save(new Usuario(
            nome, email, cpfHash, cpfCifrado, passwordEncoder.encode(request.senha()), Perfil.PACIENTE));

        Paciente paciente = new Paciente(nome, cpfHash, cpfCifrado, prioridade, agora);
        paciente.setUsuario(usuario);
        paciente.setCartaoSus(request.cartaoSus());
        paciente.setTelefone(request.telefone());
        paciente.setEmail(email);
        paciente = pacienteRepository.save(paciente);

        Consentimento consentimento = consentimentoRepository.save(
            new Consentimento(paciente, versaoTermo, true, agora));

        // Só ids no log: nome e CPF são dados pessoais.
        log.info("Autocadastro: paciente id={} (usuário id={}) aceitou o termo versão {}",
            paciente.getId(), usuario.getId(), versaoTermo);

        return new AutocadastroResponse(paciente.getId(), paciente.getNome(), paciente.getPrioridade(),
            consentimento.getVersaoTermo(), consentimento.getAceitoEm());
    }

    /**
     * Cadastro presencial feito pela recepcionista (US-05 - SCRUM-40), para quem
     * não tem celular ou não sabe usar o sistema.
     *
     * O paciente fica sem login (usuario nulo) e com cadastradoPor apontando para a
     * recepcionista. O consentimento verbal é guardado como um aceite do termo, igual
     * ao do autocadastro. Paciente e consentimento são salvos juntos: se um falhar,
     * nenhum fica salvo.
     *
     * Se o CPF já estiver cadastrado (CT-US05-02), devolve o cadastro existente com
     * jaCadastrado = true, sem criar outro e sem alterar o que já existe.
     */
    @Transactional
    public CadastroPresencialResponse cadastrarPelaRecepcao(CadastroPresencialRequest request, Long recepcionistaId) {
        // O @AssertTrue do request já barra isso; a regra fica aqui também
        // para ninguém conseguir cadastrar sem consentimento chamando o serviço direto.
        if (!request.consentimentoVerbal()) {
            throw new CadastroInvalidoException("consentimentoVerbal", CONSENTIMENTO_VERBAL_OBRIGATORIO);
        }

        String cpf = Cpf.normalizar(request.cpf());
        if (!Cpf.ehValido(cpf)) {
            throw new CadastroInvalidoException("cpf", "CPF inválido");
        }

        String cpfHash = protecaoCpf.hash(cpf);
        Optional<Paciente> existente = pacienteRepository.findByCpfHash(cpfHash);
        if (existente.isPresent()) {
            Paciente paciente = existente.get();
            log.info("Cadastro presencial: CPF já cadastrado, recepcionista id={} abriu o paciente id={}",
                recepcionistaId, paciente.getId());
            return montarResposta(paciente, cpf, true, ultimoConsentimento(paciente.getId()));
        }

        Usuario recepcionista = usuarioRepository.getReferenceById(recepcionistaId);
        Instant agora = Instant.now(clock);

        Prioridade prioridade = Prioridade.NENHUMA;
        if (request.prioridade() != null) {
            prioridade = request.prioridade();
        }

        Paciente paciente = new Paciente(request.nome().trim(), cpfHash, protecaoCpf.cifrar(cpf), prioridade, agora);
        paciente.setCartaoSus(request.cartaoSus());
        paciente.setTelefone(request.telefone());
        paciente.setCadastradoPor(recepcionista);
        paciente = pacienteRepository.save(paciente);

        Consentimento consentimento = consentimentoRepository.save(
            new Consentimento(paciente, versaoTermo, true, agora));

        // Só ids no log: nome e CPF são dados pessoais.
        log.info("Cadastro presencial: paciente id={} cadastrado pela recepcionista id={} com consentimento verbal (termo versão {})",
            paciente.getId(), recepcionistaId, versaoTermo);

        return montarResposta(paciente, cpf, false, consentimento);
    }

    /** Aceite mais recente do paciente (pode ter vindo do autocadastro ou da recepção). */
    private Consentimento ultimoConsentimento(Long pacienteId) {
        Consentimento ultimo = null;
        for (Consentimento consentimento : consentimentoRepository.findByPacienteId(pacienteId)) {
            if (ultimo == null || consentimento.getAceitoEm().isAfter(ultimo.getAceitoEm())) {
                ultimo = consentimento;
            }
        }
        return ultimo;
    }

    private CadastroPresencialResponse montarResposta(Paciente paciente, String cpf, boolean jaCadastrado,
                                                      Consentimento consentimento) {
        String versaoTermo = null;
        Instant consentimentoEm = null;
        if (consentimento != null) {
            versaoTermo = consentimento.getVersaoTermo();
            consentimentoEm = consentimento.getAceitoEm();
        }

        return new CadastroPresencialResponse(paciente.getId(), paciente.getNome(), Cpf.mascarar(cpf),
            paciente.getPrioridade(), paciente.getCriadoEm(), jaCadastrado, versaoTermo, consentimentoEm);
    }
}
