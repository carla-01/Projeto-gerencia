package br.uepb.filasus.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
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
 * Passos da US13 - Chamar o próximo (SCRUM-47), pelos endpoints das SCRUM-162 e 163.
 * O passo do horário do serviço é o de EntradaFilaSteps. Dados fictícios.
 */
public class ChamadaSteps {

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

    private Usuario profissional;
    private Servico setor;
    /** Paciente de cada número de senha, para entrar na área dele. */
    private final Map<Integer, Paciente> pacientes = new HashMap<>();
    private final Map<Integer, Usuario> logins = new HashMap<>();
    private MvcResult resposta;

    /**
     * Apaga só o que o cenário criou. Roda antes das outras limpezas (order maior), porque o
     * setor do profissional aponta para o serviço que EntradaFilaSteps apaga.
     */
    @After(order = 20000)
    public void limparDados() {
        if (profissional == null) {
            return;
        }
        senhaRepository.deleteAll();
        usuarioRepository.delete(profissional);
        for (Paciente paciente : pacientes.values()) {
            pacienteRepository.delete(paciente);
        }
        for (Usuario login : logins.values()) {
            usuarioRepository.delete(login);
        }
    }

    @Dado("que sou o profissional do setor {string}")
    public void queSouOProfissional(String nomeDoServico) {
        setor = servicoRepository.findByNome(nomeDoServico).orElseThrow();
        Usuario novo = new Usuario("Profissional Fictício", "chamada.bdd@exemplo.test",
            protecaoCpf.hash("cpf-profissional-bdd-us13"), protecaoCpf.cifrar("11144477735"), "hash",
            Perfil.PROFISSIONAL);
        novo.getSetores().add(setor);
        profissional = usuarioRepository.save(novo);
    }

    @Dado("que há um paciente presente sem prioridade com a senha número {int}")
    public void haPacientePresente(int numero) {
        criarSenha(numero, StatusSenha.PRESENTE, Prioridade.NENHUMA);
    }

    @Dado("que há um paciente presente com prioridade de idoso com a senha número {int}")
    public void haPacienteIdosoPresente(int numero) {
        criarSenha(numero, StatusSenha.PRESENTE, Prioridade.IDOSO);
    }

    @Dado("que há um paciente sem check-in com a senha número {int}")
    public void haPacienteSemCheckin(int numero) {
        criarSenha(numero, StatusSenha.AGUARDANDO, Prioridade.NENHUMA);
    }

    @Quando("chamo o próximo paciente")
    public void chamoOProximo() throws Exception {
        resposta = mockMvc.perform(post("/api/profissional/chamadas")
                .header("Authorization", "Bearer " + jwtService.gerarToken(profissional)))
            .andReturn();
        assertEquals(200, resposta.getResponse().getStatus());
    }

    @Quando("o paciente da senha número {int} tenta entrar de novo na fila")
    public void oPacienteTentaEntrarDeNovo(int numero) throws Exception {
        resposta = mockMvc.perform(post("/api/paciente/senhas")
                .header("Authorization", "Bearer " + jwtService.gerarToken(logins.get(numero)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"servicoId\": " + setor.getId() + "}"))
            .andReturn();
    }

    @Entao("a senha número {int} é chamada")
    public void aSenhaEChamada(int numero) throws Exception {
        JsonNode senha = corpo(resposta).get("senha");
        assertEquals(numero, senha.get("numero").asInt());
        assertEquals("CHAMADA", senha.get("status").asText());
    }

    @Entao("o profissional vê o aviso {string}")
    public void oProfissionalVeOAviso(String mensagem) throws Exception {
        assertEquals(mensagem, corpo(resposta).get("mensagem").asText());
    }

    @E("o paciente da senha número {int} vê na área dele a situação {string}")
    public void oPacienteVeASituacao(int numero, String situacao) throws Exception {
        assertEquals(situacao, minhaSenha(numero).get("status").asText());
    }

    @E("a área dele mostra o serviço {string} e o número {int}")
    public void aAreaMostraOServicoEONumero(String nomeDoServico, int numero) throws Exception {
        JsonNode senha = minhaSenha(numero);
        assertEquals(nomeDoServico, senha.get("servico").asText());
        assertEquals(numero, senha.get("numero").asInt());
    }

    @Entao("o sistema não cria outra senha para ele")
    public void oSistemaNaoCriaOutraSenha() {
        assertEquals(409, resposta.getResponse().getStatus());
        assertEquals(1, senhaRepository.count());
    }

    /** Paciente com login e uma senha de hoje no setor do profissional. */
    private void criarSenha(int numero, StatusSenha situacao, Prioridade prioridade) {
        String chave = "cpf-paciente-bdd-us13-" + numero;
        Usuario login = usuarioRepository.save(new Usuario("Paciente Fictício " + numero,
            "paciente" + numero + ".chamada.bdd@exemplo.test", protecaoCpf.hash(chave),
            protecaoCpf.cifrar("52998224725"), "hash", Perfil.PACIENTE));
        Paciente novo = new Paciente("Paciente Fictício " + numero, protecaoCpf.hash(chave),
            protecaoCpf.cifrar("52998224725"), Prioridade.NENHUMA, Instant.now(clock));
        novo.setUsuario(login);
        Paciente paciente = pacienteRepository.save(novo);
        logins.put(numero, login);
        pacientes.put(numero, paciente);

        Senha senha = new Senha(setor, paciente, numero, LocalDate.now(clock), prioridade, Instant.now(clock));
        senha.setStatus(situacao);
        senhaRepository.save(senha);
    }

    /** O que o GET /api/paciente/senhas mostra para o dono da senha. */
    private JsonNode minhaSenha(int numero) throws Exception {
        MvcResult minhaFila = mockMvc.perform(get("/api/paciente/senhas")
                .header("Authorization", "Bearer " + jwtService.gerarToken(logins.get(numero))))
            .andReturn();
        assertEquals(200, minhaFila.getResponse().getStatus());
        return corpo(minhaFila).get(0);
    }

    private JsonNode corpo(MvcResult resultado) throws Exception {
        // Sem charset no Content-Type o MockMvc leria em ISO-8859-1 e estragaria os acentos.
        return objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
