package br.uepb.filasus.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.uepb.filasus.auth.JwtService;
import br.uepb.filasus.fila.ConfiguracaoServico;
import br.uepb.filasus.fila.ConfiguracaoServicoRepository;
import br.uepb.filasus.fila.Servico;
import br.uepb.filasus.fila.ServicoRepository;
import br.uepb.filasus.fila.ServicoRequest;
import br.uepb.filasus.lgpd.ProtecaoCpf;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;

/** Passos da US09 - Configurar filas e horários (SCRUM-44). Dados fictícios. */
public class ConfigurarFilasSteps {

    private static final String NOME_SERVICO = "Clínico";

    /** CPF fictício, só para o hash e a cifra; não é validado aqui. */
    private static final String CPF_GESTOR = "52998224725";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ProtecaoCpf protecaoCpf;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ConfiguracaoServicoRepository configuracaoServicoRepository;

    private Usuario gestor;
    private String tokenGestor;
    private Long servicoExistenteId;
    private long quantidadeServicosAntes;
    private long quantidadeConfiguracoesAntes;
    private MvcResult resposta;

    @Before("@us09")
    public void limparServicoAntesDoCenario() {
        removerServicoDoCenario();
    }

    /** Apaga o serviço e o gestor que o cenário criou, para não atrapalhar os outros testes. */
    @After("@us09")
    public void limparDadosDepoisDoCenario() {
        removerServicoDoCenario();
        if (gestor != null) {
            usuarioRepository.delete(gestor);
            gestor = null;
        }
        tokenGestor = null;
    }

    @Dado("que estou autenticado no painel do gestor")
    public void queEstouLogadoComoGestor() {
        // Salvo no banco para o token sair com o id real do usuário no "sub".
        gestor = usuarioRepository.save(new Usuario("Gestor Fictício", "gestor.bdd@exemplo.test",
            protecaoCpf.hash(CPF_GESTOR), protecaoCpf.cifrar(CPF_GESTOR), "hash", Perfil.GESTOR));
        tokenGestor = jwtService.gerarToken(gestor);
    }

    @Dado("que já existe o serviço {string} configurado das {word} às {word} com limite de {int} senhas")
    public void queJaExisteServicoConfigurado(String nome, String abertura, String fechamento, int limite) {
        Servico servico = servicoRepository.save(new Servico(nome));
        servicoExistenteId = servico.getId();
        configuracaoServicoRepository.save(new ConfiguracaoServico(servico, LocalTime.parse(abertura),
            LocalTime.parse(fechamento), limite));
    }

    @Quando("defino o serviço {string} das {word} às {word} com limite de {int} senhas")
    public void definoServico(String nome, String abertura, String fechamento, int limite) throws Exception {
        quantidadeServicosAntes = servicoRepository.count();
        quantidadeConfiguracoesAntes = configuracaoServicoRepository.count();

        ServicoRequest request = new ServicoRequest(nome, LocalTime.parse(abertura),
            LocalTime.parse(fechamento), limite);
        resposta = mockMvc.perform(post("/api/gestor/servicos")
                .header("Authorization", "Bearer " + tokenGestor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn();
    }

    @Entao("a fila abre e fecha nesses horários e limita a {int} senhas")
    public void filaAbreFechaELimitaSenhas(int limite) throws Exception {
        assertThat(resposta.getResponse().getStatus()).isEqualTo(201);
        JsonNode corpo = corpo();
        assertThat(corpo.get("nome").asText()).isEqualTo(NOME_SERVICO);
        assertThat(corpo.get("horarioAbertura").asText()).isEqualTo("07:00");
        assertThat(corpo.get("horarioFechamento").asText()).isEqualTo("12:00");
        assertThat(corpo.get("limiteSenhasDia").asInt()).isEqualTo(limite);

        Long servicoId = corpo.get("id").asLong();
        Servico servicoSalvo = servicoRepository.findById(servicoId).orElseThrow();
        ConfiguracaoServico configuracaoSalva = configuracaoServicoRepository
            .findByServico_Id(servicoId).orElseThrow();

        assertThat(servicoSalvo.getNome()).isEqualTo(NOME_SERVICO);
        assertThat(configuracaoSalva.getHoraAbertura()).isEqualTo(LocalTime.of(7, 0));
        assertThat(configuracaoSalva.getHoraFechamento()).isEqualTo(LocalTime.of(12, 0));
        assertThat(configuracaoSalva.getLimiteSenhasDia()).isEqualTo(limite);
    }

    @Entao("recebo a mensagem {string} sem salvar ou alterar a configuração")
    public void receboMensagemSemSalvarOuAlterar(String mensagem) throws Exception {
        assertThat(resposta.getResponse().getStatus()).isEqualTo(400);
        assertThat(corpo().get("mensagem").asText()).isEqualTo(mensagem);
        assertThat(servicoRepository.count()).isEqualTo(quantidadeServicosAntes);
        assertThat(configuracaoServicoRepository.count()).isEqualTo(quantidadeConfiguracoesAntes);

        Servico servicoSalvo = servicoRepository.findById(servicoExistenteId).orElseThrow();
        ConfiguracaoServico configuracaoSalva = configuracaoServicoRepository
            .findByServico_Id(servicoExistenteId).orElseThrow();
        assertThat(servicoSalvo.getNome()).isEqualTo(NOME_SERVICO);
        assertThat(configuracaoSalva.getHoraAbertura()).isEqualTo(LocalTime.of(7, 0));
        assertThat(configuracaoSalva.getHoraFechamento()).isEqualTo(LocalTime.of(12, 0));
        assertThat(configuracaoSalva.getLimiteSenhasDia()).isEqualTo(40);
    }

    private JsonNode corpo() throws Exception {
        // Sem charset no Content-Type o MockMvc leria em ISO-8859-1 e estragaria os acentos.
        return objectMapper.readTree(resposta.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void removerServicoDoCenario() {
        servicoRepository.findByNome(NOME_SERVICO).ifPresent(servico -> {
            configuracaoServicoRepository.findByServico_Id(servico.getId())
                .ifPresent(configuracaoServicoRepository::delete);
            servicoRepository.delete(servico);
        });
        servicoExistenteId = null;
        tokenGestor = null;
        resposta = null;
    }
}
