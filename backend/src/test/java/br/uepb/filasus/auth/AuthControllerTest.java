package br.uepb.filasus.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.uepb.filasus.lgpd.ProtecaoCpf;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;
import io.jsonwebtoken.Claims;

/**
 * Testes do POST /auth/login (US01 - SCRUM-37).
 * O primeiro teste é o cenário CT-US01-01 "Login com sucesso".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProtecaoCpf protecaoCpf;

    @Autowired
    private JwtService jwtService;

    private Usuario gestor;

    /** Cria o usuário com o CPF já protegido, como a aplicação faz. */
    private Usuario novoUsuario(String cpf, String email, String senhaHash, Perfil perfil) {
        return new Usuario("Usuário de Teste", email, protecaoCpf.hash(cpf), protecaoCpf.cifrar(cpf), senhaHash, perfil);
    }

    @BeforeEach
    void cadastrarUsuario() {
        usuarioRepository.deleteAll();
        String senhaHash = passwordEncoder.encode("senha123");
        gestor = usuarioRepository.save(novoUsuario("12345678901", "gestor@ubs.com", senhaHash, Perfil.GESTOR));
    }

    /** Monta o JSON do corpo da requisição. */
    private String corpoLogin(String login, String senha) throws Exception {
        LoginRequest request = new LoginRequest(login, senha);
        return objectMapper.writeValueAsString(request);
    }

    // CT-US01-01 - Login com sucesso
    // Dado que estou cadastrado e ativo / Quando informo CPF e senha corretos /
    // Então recebo o token com o meu perfil (a tela usa o perfil para abrir a tela inicial)
    @Test
    @DisplayName("CT-US01-01 - Login com sucesso: CPF e senha corretos devolvem o token com o perfil")
    void loginComSucessoPorCpf() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoLogin("12345678901", "senha123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.perfil").value("GESTOR"))
            .andReturn();

        // O token devolvido precisa ser válido e ter o id e o perfil do usuário.
        JsonNode resposta = objectMapper.readTree(resultado.getResponse().getContentAsString());
        String token = resposta.get("token").asText();
        Claims claims = jwtService.lerToken(token);

        assertEquals(String.valueOf(gestor.getId()), claims.getSubject());
        assertEquals("GESTOR", claims.get("perfil"));
        assertNotNull(claims.getExpiration());
        // O CPF não pode aparecer no token.
        assertFalse(claims.containsKey("cpf"));
        assertFalse(claims.toString().contains("12345678901"));
    }

    @Test
    @DisplayName("CT-US01-01 - Login com sucesso: e-mail e senha corretos também entram")
    void loginComSucessoPorEmail() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoLogin("gestor@ubs.com", "senha123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.perfil").value("GESTOR"));
    }

    @Test
    void loginRecusaSenhaErrada() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoLogin("12345678901", "senhaErrada")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensagem").value("CPF ou senha inválidos"))
            .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void loginRecusaUsuarioInativo() throws Exception {
        gestor.setAtivo(false);
        usuarioRepository.save(gestor);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoLogin("12345678901", "senha123")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensagem").value("CPF ou senha inválidos"));
    }

    @Test
    void loginRecusaUsuarioInexistente() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoLogin("00000000000", "senha123")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensagem").value("CPF ou senha inválidos"));
    }
}
