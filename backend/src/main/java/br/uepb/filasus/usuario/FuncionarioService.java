package br.uepb.filasus.usuario;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uepb.filasus.fila.Servico;
import br.uepb.filasus.fila.ServicoRepository;
import br.uepb.filasus.lgpd.Cpf;
import br.uepb.filasus.lgpd.ProtecaoCpf;

/**
 * Cadastro de funcionários pelo gestor (US03 - SCRUM-15).
 *
 * O funcionário recebe acesso na hora: fica ativo e entra pelo e-mail com a
 * senha inicial informada pelo gestor (guardada só como hash bcrypt).
 * O CPF vai protegido como na US04: hash para busca e cifra para exibir.
 * O profissional fica ligado ao seu setor (profissional_servico), que a US13 usa
 * para chamar só a fila dele.
 */
@Service
public class FuncionarioService {

    /** Papéis que o gestor pode cadastrar. PACIENTE se cadastra sozinho; GESTOR não é criado pela tela. */
    static final Set<Perfil> PERFIS_PERMITIDOS = Set.of(Perfil.RECEPCIONISTA, Perfil.PROFISSIONAL);

    /** Quem aparece na lista de usuários da UBS: a equipe. Paciente fica de fora (LGPD). */
    static final Set<Perfil> PERFIS_DA_EQUIPE = Set.of(Perfil.RECEPCIONISTA, Perfil.PROFISSIONAL, Perfil.GESTOR);

    private static final Logger log = LoggerFactory.getLogger(FuncionarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProtecaoCpf protecaoCpf;
    private final ServicoRepository servicoRepository;

    public FuncionarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                              ProtecaoCpf protecaoCpf, ServicoRepository servicoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.protecaoCpf = protecaoCpf;
        this.servicoRepository = servicoRepository;
    }

    @Transactional
    public FuncionarioResponse cadastrar(CadastroFuncionarioRequest request) {
        if (!PERFIS_PERMITIDOS.contains(request.perfil())) {
            throw new PerfilNaoPermitidoException();
        }

        Servico setor = buscarSetor(request);

        String cpf = Cpf.normalizar(request.cpf());
        if (!Cpf.ehValido(cpf)) {
            throw new CpfInvalidoException();
        }

        String email = request.email().trim();
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailJaCadastradoException();
        }

        String cpfHash = protecaoCpf.hash(cpf);
        if (usuarioRepository.findByCpfHash(cpfHash).isPresent()) {
            throw new CpfJaCadastradoException();
        }

        Usuario funcionario = new Usuario(
            request.nome().trim(), email, cpfHash, protecaoCpf.cifrar(cpf),
            passwordEncoder.encode(request.senhaInicial()), request.perfil());
        funcionario.setCartaoSus(request.cartaoSus());
        if (setor != null) {
            funcionario.getSetores().add(setor);
        }
        funcionario = usuarioRepository.save(funcionario);

        // Só id e perfil no log: nome, e-mail, CPF e cartão SUS são dados pessoais.
        log.info("Funcionário cadastrado: usuário id={} perfil={}", funcionario.getId(), funcionario.getPerfil());
        return new FuncionarioResponse(funcionario);
    }

    /** Lista de usuários da UBS (Cenário 1), em ordem de nome. */
    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listar() {
        return usuarioRepository.findByPerfilInOrderByNomeAsc(PERFIS_DA_EQUIPE).stream()
            .map(FuncionarioResponse::new)
            .toList();
    }

    /** O setor é obrigatório para o profissional e não existe para os outros papéis. */
    private Servico buscarSetor(CadastroFuncionarioRequest request) {
        if (request.perfil() != Perfil.PROFISSIONAL) {
            if (request.servicoId() != null) {
                throw new SetorInvalidoException("Só o profissional de saúde tem setor");
            }
            return null;
        }
        if (request.servicoId() == null) {
            throw new SetorInvalidoException("Setor é obrigatório para o profissional");
        }
        return servicoRepository.findById(request.servicoId())
            .filter(Servico::isAtivo)
            .orElseThrow(() -> new SetorInvalidoException("Setor não encontrado"));
    }
}
