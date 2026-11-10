package br.uepb.filasus.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.uepb.filasus.auth.JwtService;
import br.uepb.filasus.fila.ConfiguracaoServicoRepository;
import br.uepb.filasus.fila.SenhaRepository;
import br.uepb.filasus.fila.ServicoRepository;
import br.uepb.filasus.paciente.ConsentimentoRepository;
import br.uepb.filasus.paciente.PacienteRepository;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AcessoNegadoPorPerfilTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ConsentimentoRepository consentimentoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ConfiguracaoServicoRepository configuracaoServicoRepository;

    @Autowired
    private SenhaRepository senhaRepository;

    static Stream<Arguments> paresDePerfilNegadosSemCoberturaAnterior() {
        return Stream.of(
            Arguments.of(Endpoint.FILA_RECEPCAO, Perfil.GESTOR),
            Arguments.of(Endpoint.FILA_RECEPCAO, Perfil.PROFISSIONAL),
            Arguments.of(Endpoint.CADASTRO_RECEPCAO, Perfil.GESTOR),
            Arguments.of(Endpoint.CADASTRO_RECEPCAO, Perfil.PROFISSIONAL),
            Arguments.of(Endpoint.CONFIGURAR_SERVICO, Perfil.PACIENTE),
            Arguments.of(Endpoint.CONFIGURAR_SERVICO, Perfil.PROFISSIONAL),
            Arguments.of(Endpoint.LISTAR_SERVICOS, Perfil.RECEPCIONISTA),
            Arguments.of(Endpoint.LISTAR_SERVICOS, Perfil.PROFISSIONAL),
            Arguments.of(Endpoint.LISTAR_SERVICOS, Perfil.GESTOR),
            Arguments.of(Endpoint.ENTRAR_NA_FILA, Perfil.GESTOR),
            Arguments.of(Endpoint.ENTRAR_NA_FILA, Perfil.PROFISSIONAL),
            Arguments.of(Endpoint.CANCELAR_SENHA, Perfil.GESTOR),
            Arguments.of(Endpoint.CANCELAR_SENHA, Perfil.PROFISSIONAL)
        );
    }

    @ParameterizedTest(name = "{0} nega acesso ao perfil {1}")
    @MethodSource("paresDePerfilNegadosSemCoberturaAnterior")
    void perfilNaoAutorizadoRecebe403SemExecutarOperacao(Endpoint endpoint, Perfil perfil) throws Exception {
        Estado estadoAntes = estadoAtual();

        MvcResult resposta = mockMvc.perform(requisicao(endpoint)
                .header("Authorization", "Bearer " + token(perfil)))
            .andReturn();

        assertThat(resposta.getResponse().getStatus())
            .as("%s deve negar %s", endpoint, perfil)
            .isEqualTo(403);
        assertThat(estadoAtual()).as("a rota negada não deve alterar dados").isEqualTo(estadoAntes);
    }

    @Test
    void listagemDeServicosSemAutenticacaoRecebe401() throws Exception {
        mockMvc.perform(get(Endpoint.LISTAR_SERVICOS.caminho))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));
    }

    @Test
    void tokenComPerfilAlteradoSemNovaAssinaturaRecebe401() throws Exception {
        String[] partes = token(Perfil.GESTOR).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        String payloadAlterado = payload.replace("\"perfil\":\"GESTOR\"", "\"perfil\":\"PACIENTE\"");
        assertThat(payloadAlterado).isNotEqualTo(payload);
        String tokenAlterado = partes[0] + "."
            + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadAlterado.getBytes(StandardCharsets.UTF_8))
            + "." + partes[2];
        Estado estadoAntes = estadoAtual();

        mockMvc.perform(requisicao(Endpoint.CONFIGURAR_SERVICO)
                .header("Authorization", "Bearer " + tokenAlterado))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));

        assertThat(estadoAtual()).isEqualTo(estadoAntes);
    }

    @Test
    void healthCheckPermiteTodosOsPerfisAutenticados() throws Exception {
        for (Perfil perfil : Perfil.values()) {
            mockMvc.perform(get("/actuator/health")
                    .header("Authorization", "Bearer " + token(perfil)))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                    .as("o health check deve exigir autenticação, não um perfil específico: %s", perfil)
                    .isEqualTo(200));
        }
    }

    private MockHttpServletRequestBuilder requisicao(Endpoint endpoint) {
        MockHttpServletRequestBuilder requisicao = switch (endpoint) {
            case FILA_RECEPCAO -> get(endpoint.caminho);
            case CADASTRO_RECEPCAO, CONFIGURAR_SERVICO, ENTRAR_NA_FILA, CANCELAR_SENHA ->
                post(endpoint.caminho).contentType(MediaType.APPLICATION_JSON).content(endpoint.corpo);
            case LISTAR_SERVICOS -> get(endpoint.caminho);
        };
        return requisicao;
    }

    private String token(Perfil perfil) {
        Usuario usuario = new Usuario("Usuário Fictício", perfil.name().toLowerCase() + ".seguranca@exemplo.test",
            "hash-cpf-" + perfil, "cpf-cifrado-de-teste", "hash-senha-de-teste", perfil);
        return jwtService.gerarToken(usuario);
    }

    private Estado estadoAtual() {
        return new Estado(usuarioRepository.count(), pacienteRepository.count(), consentimentoRepository.count(),
            servicoRepository.count(), configuracaoServicoRepository.count(), senhaRepository.count());
    }

    private record Estado(long usuarios, long pacientes, long consentimentos, long servicos,
                          long configuracoes, long senhas) {
    }

    enum Endpoint {
        FILA_RECEPCAO("/api/recepcao/fila", ""),
        CADASTRO_RECEPCAO("/api/recepcao/pacientes",
            """
                {"nome":"Paciente Fictício","cpf":"123.456.789-09","cartaoSus":"700123456789012",
                 "telefone":"83999990000","prioridade":"IDOSO","consentimentoVerbal":true}
                """),
        CONFIGURAR_SERVICO("/api/gestor/servicos",
            """
                {"nome":"Clínico de Teste","horarioAbertura":"07:00",
                 "horarioFechamento":"12:00","limiteSenhasDia":40}
                """),
        LISTAR_SERVICOS("/api/paciente/servicos", ""),
        ENTRAR_NA_FILA("/api/paciente/senhas", "{\"servicoId\":1}"),
        CANCELAR_SENHA("/api/paciente/senhas/1/cancelar", "");

        private final String caminho;
        private final String corpo;

        Endpoint(String caminho, String corpo) {
            this.caminho = caminho;
            this.corpo = corpo;
        }
    }
}
