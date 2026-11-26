package br.uepb.filasus.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.uepb.filasus.atendimento.Atendimento;
import br.uepb.filasus.atendimento.AtendimentoRepository;
import br.uepb.filasus.auth.JwtService;
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

/**
 * Passos da US14 - Iniciar e finalizar atendimento (SCRUM-48), pelos endpoints da SCRUM-167.
 * O passo do horário do serviço é o de EntradaFilaSteps. Dados fictícios.
 */
public class AtendimentoSteps {

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

    @Autowired
    private AtendimentoRepository atendimentoRepository;

    private Usuario profissional;
    private Servico setor;
    /** Senha, paciente e login de cada número de senha. */
    private final Map<Integer, Senha> senhas = new HashMap<>();
    private final Map<Integer, Paciente> pacientes = new HashMap<>();
    private final Map<Integer, Usuario> logins = new HashMap<>();
    private Long senhaChamadaId;
    private MvcResult resposta;

    /**
     * Apaga só o que o cenário criou. Roda antes das outras limpezas (order maior): o atendimento
     * aponta para a senha, que EntradaFilaSteps apaga, e o setor aponta para o serviço.
     */
    @After(order = 30000)
    public void limparDados() {
        if (profissional == null) {
            return;
        }
        atendimentoRepository.deleteAll();
        senhaRepository.deleteAll();
        usuarioRepository.delete(profissional);
        for (Paciente paciente : pacientes.values()) {
            pacienteRepository.delete(paciente);
        }
        for (Usuario login : logins.values()) {
            usuarioRepository.delete(login);
        }
    }

    @Dado("que atendo no setor {string}")
    public void queAtendoNoSetor(String nomeDoServico) {
        setor = servicoRepository.findByNome(nomeDoServico).orElseThrow();
        Usuario novo = new Usuario("Profissional Fictício", "atendimento.bdd@exemplo.test",
            protecaoCpf.hash("cpf-profissional-bdd-us14"), protecaoCpf.cifrar("11144477735"), "hash",
            Perfil.PROFISSIONAL);
        novo.getSetores().add(setor);
        profissional = usuarioRepository.save(novo);
    }

    /** O paciente faz check-in e o profissional chama pelo "Chamar próximo" (US13). */
    @Dado("que chamei o paciente da senha número {int}")
    public void queChameiOPaciente(int numero) throws Exception {
        criarSenhaPresente(numero);
        MvcResult chamada = mockMvc.perform(post("/api/profissional/chamadas")
                .header("Authorization", "Bearer " + token()))
            .andReturn();
        assertEquals(200, chamada.getResponse().getStatus());
        JsonNode senha = corpo(chamada).get("senha");
        assertEquals(numero, senha.get("numero").asInt());
        senhaChamadaId = senha.get("id").asLong();
    }

    @E("que o atendimento não foi iniciado")
    public void queOAtendimentoNaoFoiIniciado() {
        assertTrue(atendimentoRepository.findBySenha_Id(senhaChamadaId).isEmpty());
    }

    @Quando("clico em iniciar e depois em finalizar")
    public void clicoEmIniciarEDepoisEmFinalizar() throws Exception {
        MvcResult inicio = enviar("inicio");
        assertEquals(200, inicio.getResponse().getStatus());
        assertEquals("EM_ATENDIMENTO", corpo(inicio).get("status").asText());
        resposta = enviar("fim");
        assertEquals(200, resposta.getResponse().getStatus());
    }

    @Quando("tento finalizar")
    public void tentoFinalizar() throws Exception {
        resposta = enviar("fim");
    }

    @Entao("os horários de início e fim ficam gravados")
    public void osHorariosFicamGravados() throws Exception {
        Atendimento atendimento = atendimentoRepository.findBySenha_Id(senhaChamadaId).orElseThrow();
        assertEquals(Instant.now(clock), atendimento.getIniciadoEm());
        assertEquals(Instant.now(clock), atendimento.getFinalizadoEm());
        assertEquals(profissional.getId(), atendimento.getProfissional().getId());

        JsonNode corpo = corpo(resposta);
        assertNotNull(corpo.get("iniciadoEm").textValue());
        assertNotNull(corpo.get("finalizadoEm").textValue());
    }

    @Entao("o sistema pede {string}")
    public void oSistemaPede(String mensagem) throws Exception {
        assertEquals(409, resposta.getResponse().getStatus());
        assertEquals(mensagem, corpo(resposta).get("mensagem").asText());
    }

    @E("a senha número {int} fica com a situação {string}")
    public void aSenhaFicaComASituacao(int numero, String situacao) {
        Senha senha = senhaRepository.findById(senhas.get(numero).getId()).orElseThrow();
        assertEquals(StatusSenha.valueOf(situacao), senha.getStatus());
    }

    /** Na área do paciente (GET /api/paciente/senhas) só aparecem as senhas que ainda valem. */
    @E("o paciente da senha número {int} sai da fila")
    public void oPacienteSaiDaFila(int numero) throws Exception {
        MvcResult minhaFila = mockMvc.perform(get("/api/paciente/senhas")
                .header("Authorization", "Bearer " + jwtService.gerarToken(logins.get(numero))))
            .andReturn();
        assertEquals(200, minhaFila.getResponse().getStatus());
        assertEquals(0, corpo(minhaFila).size());
    }

    /** Paciente com login e uma senha de hoje, com check-in feito, no setor do profissional. */
    private void criarSenhaPresente(int numero) {
        String chave = "cpf-paciente-bdd-us14-" + numero;
        Usuario login = usuarioRepository.save(new Usuario("Paciente Fictício " + numero,
            "paciente" + numero + ".atendimento.bdd@exemplo.test", protecaoCpf.hash(chave),
            protecaoCpf.cifrar("52998224725"), "hash", Perfil.PACIENTE));
        Paciente novo = new Paciente("Paciente Fictício " + numero, protecaoCpf.hash(chave),
            protecaoCpf.cifrar("52998224725"), Prioridade.NENHUMA, Instant.now(clock));
        novo.setUsuario(login);
        Paciente paciente = pacienteRepository.save(novo);
        logins.put(numero, login);
        pacientes.put(numero, paciente);

        Senha senha = new Senha(setor, paciente, numero, LocalDate.now(clock), Prioridade.NENHUMA,
            Instant.now(clock));
        senha.setStatus(StatusSenha.PRESENTE);
        senha.setCheckinEm(Instant.now(clock));
        senhas.put(numero, senhaRepository.save(senha));
    }

    private MvcResult enviar(String acao) throws Exception {
        return mockMvc.perform(post("/api/profissional/atendimentos/" + senhaChamadaId + "/" + acao)
                .header("Authorization", "Bearer " + token()))
            .andReturn();
    }

    private String token() {
        return jwtService.gerarToken(profissional);
    }

    private JsonNode corpo(MvcResult resultado) throws Exception {
        // Sem charset no Content-Type o MockMvc leria em ISO-8859-1 e estragaria os acentos.
        return objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
