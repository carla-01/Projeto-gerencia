package br.uepb.filasus.auth;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.uepb.filasus.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Gera e lê os tokens JWT do login (US01 - SCRUM-37).
 *
 * O token leva só o id do usuário (no "sub") e o perfil.
 * O CPF NÃO vai no token, porque qualquer um consegue ler o conteúdo de um JWT.
 */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(@Value("${jwt.secret}") String segredo,
                      @Value("${jwt.expiracao-minutos:60}") long expiracaoMinutos) {
        // HS256 exige um segredo de pelo menos 32 caracteres.
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMinutos = expiracaoMinutos;
    }

    /** Cria o token assinado para o usuário que acabou de fazer login. */
    public String gerarToken(Usuario usuario) {
        Date agora = new Date();
        long expiracaoEmMilissegundos = expiracaoMinutos * 60 * 1000;
        Date expiraEm = new Date(agora.getTime() + expiracaoEmMilissegundos);

        return Jwts.builder()
            .subject(String.valueOf(usuario.getId()))
            .claim("perfil", usuario.getPerfil().name())
            .issuedAt(agora)
            .expiration(expiraEm)
            .signWith(chave)
            .compact();
    }

    /**
     * Confere a assinatura e a validade do token e devolve o conteúdo dele.
     * Se o token for inválido ou estiver expirado, lança uma JwtException.
     */
    public Claims lerToken(String token) {
        return Jwts.parser()
            .verifyWith(chave)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
