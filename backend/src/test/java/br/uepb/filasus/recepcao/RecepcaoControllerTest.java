package br.uepb.filasus.recepcao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import br.uepb.filasus.auth.JwtService;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecepcaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void CT_US02_01_recepcionistaAcessaFilaDaRecepcao() throws Exception {
        String token = token(Perfil.RECEPCIONISTA);

        mockMvc.perform(get("/api/recepcao/fila")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuarioId").isNotEmpty())
            .andExpect(jsonPath("$.fila").isEmpty());
    }

    @Test
    void CT_US02_02_pacienteRecebe403NaRecepcao() throws Exception {
        String token = token(Perfil.PACIENTE);

        mockMvc.perform(get("/api/recepcao/fila")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.mensagem").value("Acesso não permitido"));
    }

    @Test
    void requisicaoSemTokenRecebe401() throws Exception {
        mockMvc.perform(get("/api/recepcao/fila"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.mensagem").value("Não autenticado"));
    }

    private String token(Perfil perfil) {
        Usuario usuario = new Usuario("12345678901", perfil.name().toLowerCase() + "@filasus.com", "hash", perfil);
        return jwtService.gerarToken(usuario);
    }
}
