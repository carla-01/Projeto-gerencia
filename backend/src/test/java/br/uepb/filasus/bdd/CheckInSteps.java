package br.uepb.filasus.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

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
 * Passos da US10 - Check-in na recepção (SCRUM-45), pelos endpoints das SCRUM-154 e 155.
 * O passo do horário do serviço é o de EntradaFilaSteps. Dados fictícios.
 */
public class CheckInSteps {

    private static final String CPF_PACIENTE = "15350946056";
    private static final String CPF_RECEPCIONISTA = "28625587887";

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
    private Paciente paciente;
    private Senha senha;
    private MvcResult resposta;

    /** Apaga só o que o cenário criou. As senhas saem antes do paciente. */
    @After
    public void limparDados() {
        if (recepcionista == null) {
            return;
        }
        senhaRepository.deleteAll();
        if (paciente != null) {
            pacienteRepository.delete(paciente);
        }
        usuarioRepository.delete(recepcionista);
    }

    @Dado("que sou a recepcionista que faz o check-in")
    public void queSouARecepcionista() {
        recepcionista = usuarioRepository.save(new Usuario("Recepcionista Fictícia", "checkin.bdd@exemplo.test",
            protecaoCpf.hash(CPF_RECEPCIONISTA), protecaoCpf.cifrar(CPF_RECEPCIONISTA), "hash",
            Perfil.RECEPCIONISTA));
    }

    @Dado("que o paciente tem senha ativa no serviço {string}")
    public void queOPacienteTemSenhaAtiva(String nomeDoServico) {
        criarPaciente();
        senha = senhaRepository.save(new Senha(servico(nomeDoServico), paciente, 1, LocalDate.now(clock),
            Prioridade.NENHUMA, Instant.now(clock)));
    }

    @Dado("que o CPF do paciente não tem senha hoje")
    public void queOCpfNaoTemSenhaHoje() {
        criarPaciente();
    }

    @Quando("busco pelo CPF e confirmo a presença")
    public void buscoPeloCpfEConfirmo() throws Exception {
        tentoFazerOCheckin();
        long senhaId = corpo().get("senhas").get(0).get("id").asLong();

        resposta = mockMvc.perform(put("/api/recepcao/senhas/" + senhaId + "/check-in")
                .header("Authorization", "Bearer " + jwtService.gerarToken(recepcionista)))
            .andReturn();
        assertEquals(200, resposta.getResponse().getStatus());
    }

    @Quando("tento fazer o check-in pelo CPF")
    public void tentoFazerOCheckin() throws Exception {
        resposta = mockMvc.perform(post("/api/recepcao/check-in/busca")
                .header("Authorization", "Bearer " + jwtService.gerarToken(recepcionista))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"busca\": \"" + CPF_PACIENTE + "\"}"))
            .andReturn();
        assertEquals(200, resposta.getResponse().getStatus());
    }

    @Quando("coloco o paciente na fila do serviço {string}")
    public void colocoOPacienteNaFila(String nomeDoServico) throws Exception {
        // O id do paciente vem da própria busca, como na tela.
        long pacienteId = corpo().get("pacienteId").asLong();

        resposta = mockMvc.perform(post("/api/recepcao/senhas")
                .header("Authorization", "Bearer " + jwtService.gerarToken(recepcionista))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pacienteId\": " + pacienteId
                    + ", \"servicoId\": " + servico(nomeDoServico).getId() + "}"))
            .andReturn();
    }

    @Entao("o status da senha do paciente muda para {string}")
    public void oStatusDaSenhaMudaPara(String status) throws Exception {
        assertEquals(status, corpo().get("status").asText());
        Senha salva = senhaRepository.findById(senha.getId()).orElseThrow();
        assertEquals(StatusSenha.valueOf(status), salva.getStatus());
    }

    @E("o horário do check-in fica gravado")
    public void oHorarioDoCheckinFicaGravado() {
        Senha salva = senhaRepository.findById(senha.getId()).orElseThrow();
        assertEquals(Instant.now(clock), salva.getCheckinEm());
    }

    @Entao("a recepção vê o aviso {string}")
    public void aRecepcaoVeOAviso(String mensagem) throws Exception {
        assertEquals(mensagem, corpo().get("mensagem").asText());
        assertEquals(0, corpo().get("senhas").size());
    }

    @Entao("o paciente recebe a senha número {int} com o status {string}")
    public void oPacienteRecebeASenha(int numero, String status) throws Exception {
        assertEquals(201, resposta.getResponse().getStatus());
        assertEquals(numero, corpo().get("numero").asInt());
        assertEquals(status, corpo().get("status").asText());
    }

    /** Paciente cadastrado no balcão: sem login, como na US05. */
    private void criarPaciente() {
        paciente = pacienteRepository.save(new Paciente("Paciente Fictício", protecaoCpf.hash(CPF_PACIENTE),
            protecaoCpf.cifrar(CPF_PACIENTE), Prioridade.NENHUMA, Instant.now(clock)));
    }

    private Servico servico(String nome) {
        return servicoRepository.findByNome(nome).orElseThrow();
    }

    private JsonNode corpo() throws Exception {
        // Sem charset no Content-Type o MockMvc leria em ISO-8859-1 e estragaria os acentos.
        return objectMapper.readTree(resposta.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
