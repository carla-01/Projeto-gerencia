package br.uepb.filasus.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
import br.uepb.filasus.fila.Senha;
import br.uepb.filasus.fila.SenhaRepository;
import br.uepb.filasus.fila.Servico;
import br.uepb.filasus.fila.ServicoRepository;
import br.uepb.filasus.fila.StatusSenha;
import br.uepb.filasus.lgpd.ProtecaoCpf;
import br.uepb.filasus.paciente.Paciente;
import br.uepb.filasus.paciente.PacienteRepository;
import br.uepb.filasus.paciente.Prioridade;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;
import io.cucumber.java.After;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;

/** Passos da US06 - Entrar na fila (SCRUM-41), pelo endpoint da SCRUM-136. Dados fictícios. */
public class EntradaFilaSteps {

    /** CPF fictício, só para o hash e a cifra; não é validado aqui. */
    private static final String CPF = "71428793860";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ProtecaoCpf protecaoCpf;

    @Autowired
    private Clock clock;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ConfiguracaoServicoRepository configuracaoServicoRepository;

    @Autowired
    private SenhaRepository senhaRepository;

    private Usuario usuario;
    private Paciente paciente;
    private MvcResult resposta;

    /** Apaga só o que o cenário criou, para não atrapalhar os outros testes. */
    @After
    public void limparDados() {
        senhaRepository.deleteAll();
        configuracaoServicoRepository.deleteAll();
        servicoRepository.deleteAll();
        if (paciente != null) {
            pacienteRepository.delete(paciente);
        }
        if (usuario != null) {
            usuarioRepository.delete(usuario);
        }
    }

    @Dado("que sou um paciente cadastrado e logado")
    public void queSouUmPacienteCadastradoELogado() {
        usuario = usuarioRepository.save(new Usuario("Paciente Fictício", "paciente.bdd@exemplo.test",
            protecaoCpf.hash(CPF), protecaoCpf.cifrar(CPF), "hash", Perfil.PACIENTE));
        Paciente novoPaciente = new Paciente("Paciente Fictício", protecaoCpf.hash(CPF), protecaoCpf.cifrar(CPF),
            Prioridade.NENHUMA, Instant.now(clock));
        novoPaciente.setUsuario(usuario);
        paciente = pacienteRepository.save(novoPaciente);
    }

    @Dado("que o serviço {string} funciona das {word} às {word} com limite de {int} senha(s)")
    public void queOServicoFunciona(String nome, String abertura, String fechamento, int limite) {
        Servico servico = servicoRepository.save(new Servico(nome));
        configuracaoServicoRepository.save(new ConfiguracaoServico(servico,
            LocalTime.parse(abertura), LocalTime.parse(fechamento), limite));
    }

    @E("já foi emitida {int} senha(s) hoje no serviço {string}")
    public void jaFoiEmitidaSenhaHoje(int quantidade, String nome) {
        Servico servico = servico(nome);
        for (int numero = 1; numero <= quantidade; numero++) {
            // Senha já atendida: conta no limite do dia, mas não é uma senha ativa do paciente.
            Senha senha = new Senha(servico, paciente, numero, LocalDate.now(clock), Prioridade.NENHUMA,
                Instant.now(clock));
            senha.setStatus(StatusSenha.ATENDIDA);
            senhaRepository.save(senha);
        }
    }

    @E("já entrei na fila do serviço {string}")
    public void jaEntreiNaFila(String nome) throws Exception {
        entroNaFila(nome);
        assertThat(resposta.getResponse().getStatus()).isEqualTo(201);
    }

    @Quando("entro na fila do serviço {string}")
    public void entroNaFila(String nome) throws Exception {
        resposta = mockMvc.perform(post("/api/paciente/senhas")
                .header("Authorization", "Bearer " + jwtService.gerarToken(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"servicoId\": " + servico(nome).getId() + "}"))
            .andReturn();
    }

    @Entao("recebo a senha número {int} do serviço {string}")
    public void receboASenha(int numero, String nome) throws Exception {
        assertThat(resposta.getResponse().getStatus()).isEqualTo(201);
        JsonNode senha = corpo();
        assertThat(senha.get("numero").asInt()).isEqualTo(numero);
        assertThat(senha.get("servico").asText()).isEqualTo(nome);
        assertThat(senha.get("data").asText()).isEqualTo(LocalDate.now(clock).toString());
    }

    @E("a senha fica com o status {string}")
    public void aSenhaFicaComOStatus(String status) throws Exception {
        assertThat(corpo().get("status").asText()).isEqualTo(status);
    }

    @Entao("vejo a mensagem {string}")
    public void vejoAMensagem(String mensagem) throws Exception {
        assertThat(resposta.getResponse().getStatus()).isBetween(400, 499);
        assertThat(corpo().get("mensagem").asText()).isEqualTo(mensagem);
    }

    @E("nenhuma senha é gerada para mim")
    public void nenhumaSenhaEGerada() {
        boolean temSenhaAtiva = senhaRepository.findAll().stream()
            .anyMatch(senha -> senha.getStatus() == StatusSenha.AGUARDANDO);
        assertThat(temSenhaAtiva).isFalse();
    }

    private Servico servico(String nome) {
        return servicoRepository.findByNome(nome).orElseThrow();
    }

    private JsonNode corpo() throws Exception {
        // Sem charset no Content-Type o MockMvc leria em ISO-8859-1 e estragaria os acentos.
        return objectMapper.readTree(resposta.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
