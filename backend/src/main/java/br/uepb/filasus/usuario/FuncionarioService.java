package br.uepb.filasus.usuario;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uepb.filasus.lgpd.Cpf;
import br.uepb.filasus.lgpd.ProtecaoCpf;

/**
 * Cadastro de funcionários pelo gestor (US03 - SCRUM-15).
 *
 * O funcionário recebe acesso na hora: fica ativo e entra pelo e-mail com a
 * senha inicial informada pelo gestor (guardada só como hash bcrypt).
 * O CPF vai protegido como na US04: hash para busca e cifra para exibir.
 */
@Service
public class FuncionarioService {

    /** Papéis que o gestor pode cadastrar. PACIENTE se cadastra sozinho; GESTOR não é criado pela tela. */
    static final Set<Perfil> PERFIS_PERMITIDOS = Set.of(Perfil.RECEPCIONISTA, Perfil.PROFISSIONAL);

    private static final Logger log = LoggerFactory.getLogger(FuncionarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProtecaoCpf protecaoCpf;

    public FuncionarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                              ProtecaoCpf protecaoCpf) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.protecaoCpf = protecaoCpf;
    }

    @Transactional
    public FuncionarioResponse cadastrar(CadastroFuncionarioRequest request) {
        if (!PERFIS_PERMITIDOS.contains(request.perfil())) {
            throw new PerfilNaoPermitidoException();
        }

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
        funcionario = usuarioRepository.save(funcionario);

        // Só id e perfil no log: nome, e-mail, CPF e cartão SUS são dados pessoais.
        log.info("Funcionário cadastrado: usuário id={} perfil={}", funcionario.getId(), funcionario.getPerfil());
        return new FuncionarioResponse(funcionario);
    }
}
