package br.uepb.filasus.config;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);
    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        String usuario = autenticacao == null ? "desconhecido" : String.valueOf(autenticacao.getPrincipal());
        String perfil = perfil(autenticacao == null ? null : autenticacao.getAuthorities());

        log.warn("Acesso negado: usuario={}, perfil={}, metodo={}, rota={}, dataHora={}",
            usuario, perfil, request.getMethod(), request.getRequestURI(), Instant.now());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new RespostaAcessoNegado());
    }

    private String perfil(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) {
            return "desconhecido";
        }

        return authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> authority.substring("ROLE_".length()))
            .findFirst()
            .orElse("desconhecido");
    }

    private record RespostaAcessoNegado(int status, String mensagem) {
        private RespostaAcessoNegado() {
            this(403, "Acesso não permitido");
        }
    }
}
