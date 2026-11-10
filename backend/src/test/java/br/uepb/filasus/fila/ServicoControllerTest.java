package br.uepb.filasus.fila;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import br.uepb.filasus.auth.JwtService;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ConfiguracaoServicoRepository configuracaoServicoRepository;

    @BeforeEach
    void limparDados() {
        configuracaoServicoRepository.deleteAll();
        servicoRepository.deleteAll();
    }

    @Test
    void gestorSalvaServicoComHorarios() throws Exception {
        mockMvc.perform(post("/api/gestor/servicos")
                .header("Authorization", "Bearer " + token(Perfil.GESTOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("07:00", "12:00")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nome").value("Clínico"))
            .andExpect(jsonPath("$.horarioAbertura").value("07:00"))
            .andExpect(jsonPath("$.horarioFechamento").value("12:00"))
            .andExpect(jsonPath("$.limiteSenhasDia").value(20));

        assertEquals(1, servicoRepository.count());
        assertEquals(1, configuracaoServicoRepository.count());
        assertEquals(20, configuracaoServicoRepository.findAll().get(0).getLimiteSenhasDia());
    }

    @Test
    void nomeDuplicadoRecebe409() throws Exception {
        mockMvc.perform(post("/api/gestor/servicos")
                .header("Authorization", "Bearer " + token(Perfil.GESTOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("07:00", "12:00")))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/gestor/servicos")
                .header("Authorization", "Bearer " + token(Perfil.GESTOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("08:00", "13:00")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Serviço já cadastrado"));
    }

    @Test
    void nomeEmBrancoRecebe400ComMensagem() throws Exception {
        mockMvc.perform(post("/api/gestor/servicos")
                .header("Authorization", "Bearer " + token(Perfil.GESTOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nome": " ",
                      "horarioAbertura": "07:00",
                      "horarioFechamento": "12:00",
                      "limiteSenhasDia": 20
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("nome"))
            .andExpect(jsonPath("$.mensagem").value("Nome do serviço é obrigatório"));
    }

    @Test
    void limiteZeroOuNegativoRecebe400() throws Exception {
        validarLimiteNaoSalva(0);
        validarLimiteNaoSalva(-1);
    }

    @Test
    void fimAntesDoInicioNaoSalva() throws Exception {
        validarHorarioNaoSalva("12:00", "07:00");
    }

    @Test
    void fimIgualAoInicioNaoSalva() throws Exception {
        validarHorarioNaoSalva("07:00", "07:00");
    }

    @Test
    void outroPerfilRecebe403() throws Exception {
        mockMvc.perform(post("/api/gestor/servicos")
                .header("Authorization", "Bearer " + token(Perfil.RECEPCIONISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("07:00", "12:00")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mensagem").value("Acesso não permitido"));

        assertEquals(0, servicoRepository.count());
        assertEquals(0, configuracaoServicoRepository.count());
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mockMvc.perform(post("/api/gestor/servicos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("07:00", "12:00")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensagem").value("Não autenticado"));

        assertEquals(0, servicoRepository.count());
        assertEquals(0, configuracaoServicoRepository.count());
    }

    private void validarHorarioNaoSalva(String abertura, String fechamento) throws Exception {
        mockMvc.perform(post("/api/gestor/servicos")
                .header("Authorization", "Bearer " + token(Perfil.GESTOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(abertura, fechamento)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem").value("Horário inválido"));

        assertEquals(0, servicoRepository.count());
        assertEquals(0, configuracaoServicoRepository.count());
    }

    private void validarLimiteNaoSalva(int limite) throws Exception {
        mockMvc.perform(post("/api/gestor/servicos")
                .header("Authorization", "Bearer " + token(Perfil.GESTOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("07:00", "12:00", limite)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("limiteSenhasDia"))
            .andExpect(jsonPath("$.mensagem").value("Limite de senhas por dia deve ser maior que zero"));

        assertEquals(0, servicoRepository.count());
        assertEquals(0, configuracaoServicoRepository.count());
    }

    private String corpo(String abertura, String fechamento) {
        return corpo(abertura, fechamento, 20);
    }

    private String corpo(String abertura, String fechamento, int limite) {
        return """
            {
              "nome": "Clínico",
              "horarioAbertura": "%s",
              "horarioFechamento": "%s",
              "limiteSenhasDia": %d
            }
            """.formatted(abertura, fechamento, limite);
    }

    private String token(Perfil perfil) {
        Usuario usuario = new Usuario("Usuário de Teste", perfil.name().toLowerCase() + "@filasus.com",
            "cpf-hash", "cpf-cifrado", "hash", perfil);
        return jwtService.gerarToken(usuario);
    }
}
