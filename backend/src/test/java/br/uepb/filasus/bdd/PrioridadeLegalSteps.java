package br.uepb.filasus.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.uepb.filasus.auth.JwtService;
import br.uepb.filasus.fila.Senha;
import br.uepb.filasus.fila.SenhaRepository;
import br.uepb.filasus.fila.ServicoRepository;
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

/**
 * Passos da US11 - Aplicar prioridade legal (SCRUM-46), pelos endpoints das SCRUM-158 e 159.
 * O passo do horário do serviço é o de EntradaFilaSteps. Dados fictícios.
 */
public class PrioridadeLegalSteps {

    /** CPFs fictícios, só para o hash e a cifra; não são validados aqui. */
    private static final String CPF_RECEPCIONISTA = "71645382090";
    private static final String CPF_PACIENTE = "38207164593";
    private static final String[] CPFS_DOS_OUTROS = {"50918273644", "82736450127"};

    /** Texto da tela da recepção para o valor da prioridade (SCRUM-160). */
    private static final Map<String, String> PRIORIDADE_DA_TELA = Map.of(
        "Idoso 60+", "IDOSO",
        "Gestante", "GESTANTE",
        "Pessoa com deficiência", "PCD",
        "Urgência", "URGENCIA");

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
    private SenhaRepository senhaRepository;

    private Usuario recepcionista;
    private Usuario usuarioPaciente;
    private Paciente paciente;
    private final List<Paciente> outrosPacientes = new ArrayList<>();
    private Senha senha;
    private MvcResult resposta;

    /** Apaga só o que o cenário criou. As senhas saem antes, porque apontam para pacientes e recepcionista. */
    @After
    public void limparDados() {
        if (recepcionista == null) {
            return;
        }
        senhaRepository.deleteAll();
        outrosPacientes.forEach(pacienteRepository::delete);
        if (paciente != null) {
            pacienteRepository.delete(paciente);
            usuarioRepository.delete(usuarioPaciente);
        }
        usuarioRepository.delete(recepcionista);
    }

    @Dado("que sou a recepcionista que marca a prioridade")
    public void queSouARecepcionista() {
        recepcionista = usuarioRepository.save(new Usuario("Recepcionista Fictícia", "prioridade.bdd@exemplo.test",
            protecaoCpf.hash(CPF_RECEPCIONISTA), protecaoCpf.cifrar(CPF_RECEPCIONISTA), "hash",
            Perfil.RECEPCIONISTA));
    }

    @Dado("que o paciente está na fila do serviço {string} com a senha número {int}")
    public void queOPacienteEstaNaFila(String nomeDoServico, int numero) {
        // Paciente com login, para conferir a posição que ele vê (US07).
        usuarioPaciente = usuarioRepository.save(new Usuario("Paciente Fictício", "paciente.prioridade.bdd@exemplo.test",
            protecaoCpf.hash(CPF_PACIENTE), protecaoCpf.cifrar(CPF_PACIENTE), "hash", Perfil.PACIENTE));
        Paciente novo = new Paciente("Paciente Fictício", protecaoCpf.hash(CPF_PACIENTE),
            protecaoCpf.cifrar(CPF_PACIENTE), Prioridade.NENHUMA, Instant.now(clock));
        novo.setUsuario(usuarioPaciente);
        paciente = pacienteRepository.save(novo);

        senha = senhaRepository.save(new Senha(servicoRepository.findByNome(nomeDoServico).orElseThrow(), paciente,
            numero, LocalDate.now(clock), Prioridade.NENHUMA, Instant.now(clock)));
    }

    @E("há {int} pessoa(s) sem prioridade na frente dele")
    public void haPessoasSemPrioridadeNaFrente(int quantidade) {
        for (int i = 0; i < quantidade; i++) {
            String cpf = CPFS_DOS_OUTROS[i];
            // Paciente cadastrado pela recepção, sem login próprio; pega as senhas antes da dele.
            Paciente outro = pacienteRepository.save(new Paciente("Outro Paciente " + (i + 1),
                protecaoCpf.hash(cpf), protecaoCpf.cifrar(cpf), Prioridade.NENHUMA, Instant.now(clock)));
            outrosPacientes.add(outro);
            senhaRepository.save(new Senha(senha.getServico(), outro, i + 1, LocalDate.now(clock),
                Prioridade.NENHUMA, Instant.now(clock)));
        }
    }

    @Quando("marco a prioridade {string}")
    public void marcoAPrioridade(String prioridade) throws Exception {
        marcar("{\"prioridade\": \"" + PRIORIDADE_DA_TELA.get(prioridade) + "\"}");
        assertEquals(200, resposta.getResponse().getStatus());
    }

    @Quando("tento marcar a prioridade {string} sem informar o motivo")
    public void tentoMarcarSemMotivo(String prioridade) throws Exception {
        marcar("{\"prioridade\": \"" + PRIORIDADE_DA_TELA.get(prioridade) + "\", \"motivo\": \"\"}");
    }

    @Entao("a senha fica com a prioridade {string}")
    public void aSenhaFicaComAPrioridade(String prioridade) throws Exception {
        assertEquals(prioridade, corpo(resposta).get("prioridade").asText());
        assertEquals(Prioridade.valueOf(prioridade), senhaNoBanco().getPrioridadeAplicada());
    }

    @E("a mudança fica registrada com a recepcionista e o horário")
    public void aMudancaFicaRegistrada() {
        Senha salva = senhaNoBanco();
        assertEquals(recepcionista.getId(), salva.getPrioridadePor().getId());
        assertEquals(Instant.now(clock), salva.getPrioridadeEm());
    }

    @E("o paciente passa para a posição {int} com {int} pessoa(s) na frente")
    public void oPacientePassaParaAPosicao(int posicao, int pessoasAFrente) throws Exception {
        MvcResult minhaFila = mockMvc.perform(get("/api/paciente/senhas")
                .header("Authorization", "Bearer " + jwtService.gerarToken(usuarioPaciente)))
            .andReturn();
        assertEquals(200, minhaFila.getResponse().getStatus());

        JsonNode minhaSenha = corpo(minhaFila).get(0);
        assertEquals(senha.getNumero(), minhaSenha.get("numero").asInt());
        assertEquals(posicao, minhaSenha.get("posicao").asInt());
        assertEquals(pessoasAFrente, minhaSenha.get("pessoasAFrente").asInt());
    }

    @Entao("o sistema exige o motivo com a mensagem {string}")
    public void oSistemaExigeOMotivo(String mensagem) throws Exception {
        assertEquals(400, resposta.getResponse().getStatus());
        assertEquals("motivo", corpo(resposta).get("campo").asText());
        assertEquals(mensagem, corpo(resposta).get("mensagem").asText());
    }

    @E("a senha continua sem prioridade")
    public void aSenhaContinuaSemPrioridade() {
        Senha salva = senhaNoBanco();
        assertEquals(Prioridade.NENHUMA, salva.getPrioridadeAplicada());
        assertNull(salva.getPrioridadeEm());
    }

    private void marcar(String corpo) throws Exception {
        resposta = mockMvc.perform(put("/api/recepcao/senhas/" + senha.getId() + "/prioridade")
                .header("Authorization", "Bearer " + jwtService.gerarToken(recepcionista))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo))
            .andReturn();
    }

    private Senha senhaNoBanco() {
        return senhaRepository.findById(senha.getId()).orElseThrow();
    }

    private JsonNode corpo(MvcResult resultado) throws Exception {
        // Sem charset no Content-Type o MockMvc leria em ISO-8859-1 e estragaria os acentos.
        return objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
