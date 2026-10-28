package br.uepb.filasus.auth;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;

/**
 * Regras do login (US01 - SCRUM-37).
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Confere login e senha e devolve o token.
     * Lança LoginInvalidoException se o usuário não existir, estiver inativo
     * ou se a senha estiver errada.
     */
    public LoginResponse login(LoginRequest request) {
        Optional<Usuario> encontrado = buscarUsuario(request.login());

        if (encontrado.isEmpty()) {
            throw new LoginInvalidoException();
        }

        Usuario usuario = encontrado.get();

        if (!usuario.isAtivo()) {
            throw new LoginInvalidoException();
        }

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new LoginInvalidoException();
        }

        String token = jwtService.gerarToken(usuario);
        return new LoginResponse(token, usuario.getPerfil());
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
