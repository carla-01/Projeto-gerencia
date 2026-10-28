package br.uepb.filasus.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;

/**
 * Testes do bloqueio contra força bruta no login (DA-SEG-I2-01).
 * O primeiro teste é o cenário CT-US01-02 "Senha incorreta".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginBloqueioTest {

    private static final Instant INICIO = Instant.parse("2026-10-27T12:00:00Z");

    /** Troca o relógio de verdade pelo relógio simulado só neste teste. */
    @TestConfiguration
    static class RelogioDeTesteConfig {

        @Bean
        @Primary
        RelogioAjustavel relogioAjustavel() {
            return new RelogioAjustavel(INICIO);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RelogioAjustavel relogio;

    @BeforeEach
    void prepararCenario() {
        relogio.definir(INICIO);
        usuarioRepository.deleteAll();
        String senhaHash = passwordEncoder.encode("senha123");
        usuarioRepository.save(new Usuario("12345678901", "paciente@email.com", senhaHash, Perfil.PACIENTE));
    }

    /** Faz um POST /auth/login com o CPF do usuário do cenário. */
    private ResultActions tentarLogin(String senha) throws Exception {
        LoginRequest request = new LoginRequest("12345678901", senha);
        return mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
    }

    /** Erra a senha várias vezes, conferindo que cada tentativa dá 401. */
    private void errarSenha(int vezes) throws Exception {
        for (int i = 0; i < vezes; i++) {
            tentarLogin("senhaErrada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("CPF ou senha inválidos"));
        }
    }

    private Usuario buscarUsuario() {
        return usuarioRepository.findByCpf("12345678901").get();
    }

    // CT-US01-02 - Senha incorreta
    // Dado que estou cadastrado / Quando informo a senha errada 5 vezes seguidas /
    // Então vejo "CPF ou senha inválidos" e a conta fica bloqueada por 15 minutos
    @Test
    @DisplayName("CT-US01-02 - Senha incorreta: 5 senhas erradas mostram 'CPF ou senha inválidos' e bloqueiam a conta")
    void cincoSenhasErradasBloqueiamAContaPor15Minutos() throws Exception {
        errarSenha(5);

        // A conta ficou bloqueada exatamente até 15 minutos depois da 5ª tentativa.
        Usuario usuario = buscarUsuario();
        assertNotNull(usuario.getBloqueadoAte());
        assertEquals(INICIO.plus(Duration.ofMinutes(15)), usuario.getBloqueadoAte());

        // Mesmo com a senha certa, o login é recusado com a mensagem genérica.
        tentarLogin("senha123")
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensagem").value("CPF ou senha inválidos"))
            .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("CT-US01-02 - Senha incorreta: o bloqueio dura 15 minutos")
    void contaContinuaBloqueadaAos14MinutosELiberaAos15() throws Exception {
        errarSenha(5);

        relogio.avancar(Duration.ofMinutes(14));
        tentarLogin("senha123")
            .andExpect(status().isUnauthorized());

        relogio.avancar(Duration.ofMinutes(1));
        tentarLogin("senha123")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());

        // Depois do login certo, o bloqueio e o contador são limpos.
        Usuario usuario = buscarUsuario();
        assertNull(usuario.getBloqueadoAte());
        assertEquals(0, usuario.getTentativasFalhas());
    }

    @Test
    void quatroSenhasErradasNaoBloqueiam() throws Exception {
        errarSenha(4);

        assertEquals(4, buscarUsuario().getTentativasFalhas());

        tentarLogin("senha123")
            .andExpect(status().isOk());
    }

    @Test
    void loginCertoZeraAsTentativasErradas() throws Exception {
        // 4 erradas + 1 certa + 4 erradas: nunca são 5 seguidas, então não bloqueia.
        errarSenha(4);
        tentarLogin("senha123").andExpect(status().isOk());
        errarSenha(4);

        tentarLogin("senha123")
            .andExpect(status().isOk());
    }

    @Test
    void depoisDoBloqueioOUsuarioTemCincoTentativasDeNovo() throws Exception {
        errarSenha(5);
        relogio.avancar(Duration.ofMinutes(15));

        // Passou o bloqueio: 5 novas senhas erradas bloqueiam de novo.
        errarSenha(5);

        Usuario usuario = buscarUsuario();
        Instant esperado = INICIO.plus(Duration.ofMinutes(30));
        assertEquals(esperado, usuario.getBloqueadoAte());

        tentarLogin("senha123")
            .andExpect(status().isUnauthorized());
    }
}
