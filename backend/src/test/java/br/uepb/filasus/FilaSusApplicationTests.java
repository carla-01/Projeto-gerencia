package br.uepb.filasus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import br.uepb.filasus.auth.JwtService;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FilaSusApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void contextoSobe() {
        // Garante que a aplicação inicia com o perfil de teste.
    }

    @Test
    void healthCheckResponde() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.mensagem").value("Não autenticado"));
    }

    @Test
    void healthCheckComTokenValido() throws Exception {
        Usuario usuario = new Usuario("Gestor de Teste", "gestor@ubs.com", "cpf-hash", "cpf-cifrado", "hash", Perfil.GESTOR);
        String token = jwtService.gerarToken(usuario);

        mockMvc.perform(get("/actuator/health")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void tokenInvalidoDevolveNaoAutenticado() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .header("Authorization", "Bearer token-invalido"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.mensagem").value("Não autenticado"));
    }
}
