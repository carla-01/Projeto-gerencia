package br.uepb.filasus.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;

/**
 * Regras do login (US01 - SCRUM-37).
 *
 * Inclui o bloqueio contra força bruta (DA-SEG-I2-01): depois de 5 senhas
 * erradas seguidas, a conta fica bloqueada por 15 minutos.
 *
 * Atenção: este método NÃO pode ser @Transactional. Se fosse, a exceção de
 * login inválido desfaria o save() da tentativa errada e o contador nunca subiria.
 */
@Service
public class AuthService {

    /** Quantas senhas erradas seguidas bloqueiam a conta. */
    static final int MAXIMO_TENTATIVAS = 5;

    /** Quanto tempo a conta fica bloqueada. */
    static final Duration TEMPO_BLOQUEIO = Duration.ofMinutes(15);

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    /**
     * Confere login e senha e devolve o token.
     * Lança LoginInvalidoException se o usuário não existir, estiver bloqueado,
     * estiver inativo ou se a senha estiver errada. A mensagem é sempre a mesma.
     */
    public LoginResponse login(LoginRequest request) {
        Optional<Usuario> encontrado = buscarUsuario(request.login());

        if (encontrado.isEmpty()) {
            // Não colocamos o CPF/e-mail digitado no log (dado pessoal).
            log.warn("Login recusado: usuário não encontrado");
            throw new LoginInvalidoException();
        }

        Usuario usuario = encontrado.get();
        Instant agora = Instant.now(clock);

        if (estaBloqueado(usuario, agora)) {
            log.warn("Login recusado: usuário id={} está bloqueado até {}", usuario.getId(), usuario.getBloqueadoAte());
            throw new LoginInvalidoException();
        }

        if (!usuario.isAtivo()) {
            log.warn("Login recusado: usuário id={} está inativo", usuario.getId());
            throw new LoginInvalidoException();
        }

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            registrarSenhaErrada(usuario, agora);
            throw new LoginInvalidoException();
        }

        // Login certo: zera o contador de senhas erradas.
        if (usuario.getTentativasFalhas() > 0 || usuario.getBloqueadoAte() != null) {
            usuario.setTentativasFalhas(0);
            usuario.setBloqueadoAte(null);
            usuarioRepository.save(usuario);
        }

        log.info("Login com sucesso: usuário id={}", usuario.getId());
        String token = jwtService.gerarToken(usuario);
        return new LoginResponse(token, usuario.getPerfil());
    }

    /** A conta está bloqueada se tiver um "bloqueado até" que ainda não passou. */
    private boolean estaBloqueado(Usuario usuario, Instant agora) {
        Instant bloqueadoAte = usuario.getBloqueadoAte();

        if (bloqueadoAte == null) {
            return false;
        }

        if (agora.isBefore(bloqueadoAte)) {
            return true;
        }

        return false;
    }

    /** Soma uma senha errada e bloqueia a conta quando chegar a 5. */
    private void registrarSenhaErrada(Usuario usuario, Instant agora) {
        int tentativas = usuario.getTentativasFalhas() + 1;

        if (tentativas >= MAXIMO_TENTATIVAS) {
            // Bloqueia e zera o contador: depois dos 15 minutos o usuário
            // tem de novo 5 tentativas.
            Instant bloqueadoAte = agora.plus(TEMPO_BLOQUEIO);
            usuario.setBloqueadoAte(bloqueadoAte);
            usuario.setTentativasFalhas(0);
            log.warn("Senha errada: usuário id={} (tentativa {} de {}). Conta bloqueada até {}",
                usuario.getId(), tentativas, MAXIMO_TENTATIVAS, bloqueadoAte);
        } else {
            usuario.setTentativasFalhas(tentativas);
            log.warn("Senha errada: usuário id={} (tentativa {} de {})",
                usuario.getId(), tentativas, MAXIMO_TENTATIVAS);
        }

        usuarioRepository.save(usuario);
    }

    /** Se o login tiver "@" é um e-mail; senão, é um CPF. */
    private Optional<Usuario> buscarUsuario(String login) {
        String loginLimpo = login.trim();

        if (loginLimpo.contains("@")) {
            return usuarioRepository.findByEmail(loginLimpo);
        }

        // O CPF é salvo só com números, então tiramos pontos e traço
        // caso o usuário digite "123.456.789-01".
        String cpf = loginLimpo.replace(".", "").replace("-", "");
        return usuarioRepository.findByCpf(cpf);
    }
}
