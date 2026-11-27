package br.uepb.filasus.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

/** Passos da US07 - Ver minha posição (SCRUM-42), pelo endpoint da SCRUM-139. Dados fictícios. */
public class PosicaoFilaSteps {

    /** CPFs fictícios, só para o hash e a cifra; não são validados aqui. */
    private static final String CPF = "27493186050";
    private static final String[] CPFS_DOS_OUTROS = {"61839205740", "90572618303"};

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
    private final List<Paciente> outrosPacientes = new ArrayList<>();
    private Servico servico;
    private MvcResult resposta;

    /** Apaga só o que o cenário criou. As senhas saem antes, porque apontam para os pacientes. */
    @After
    public void limparDados() {
        if (paciente == null) {
            return;
        }
        senhaRepository.deleteAll();
        configuracaoServicoRepository.deleteAll();
        servicoRepository.deleteAll();
        outrosPacientes.forEach(pacienteRepository::delete);
        pacienteRepository.delete(paciente);
        usuarioRepository.delete(usuario);
    }

    @Dado("que tenho a senha número {int} na fila do serviço {string}")
    public void queTenhoASenhaNaFila(int numero, String nomeDoServico) {
        criarPacienteEServico(nomeDoServico);
        senhaRepository.save(novaSenha(paciente, numero, StatusSenha.AGUARDANDO));
    }

    @E("há {int} pessoa(s) aguardando na minha frente")
    public void haPessoasAguardandoNaFrente(int quantidade) {
        for (int i = 0; i < quantidade; i++) {
            String cpf = CPFS_DOS_OUTROS[i];
            // Paciente cadastrado pela recepção, sem login próprio; pega as senhas antes da minha.
            Paciente outro = pacienteRepository.save(new Paciente("Outro Paciente " + (i + 1),
                protecaoCpf.hash(cpf), protecaoCpf.cifrar(cpf), Prioridade.NENHUMA, Instant.now(clock)));
            outrosPacientes.add(outro);
            senhaRepository.save(novaSenha(outro, i + 1, StatusSenha.AGUARDANDO));
        }
    }

    @Dado("que já fui atendido hoje no serviço {string} e não tenho senha ativa")
    public void queJaFuiAtendidoHoje(String nomeDoServico) {
        criarPacienteEServico(nomeDoServico);
        senhaRepository.save(novaSenha(paciente, 1, StatusSenha.ATENDIDA));
    }

    @Quando("abro a minha fila")
    public void abroAMinhaFila() throws Exception {
        resposta = consultar("/api/paciente/senhas");
    }

    @Entao("vejo a senha número {int} do serviço {string}")
    public void vejoASenha(int numero, String nomeDoServico) throws Exception {
        assertThat(resposta.getResponse().getStatus()).isEqualTo(200);
        JsonNode senhas = corpo(resposta);
        assertThat(senhas).hasSize(1);
        assertThat(senhas.get(0).get("numero").asInt()).isEqualTo(numero);
        assertThat(senhas.get(0).get("servico").asText()).isEqualTo(nomeDoServico);
        assertThat(senhas.get(0).get("status").asText()).isEqualTo("AGUARDANDO");
    }

    @E("estou na posição {int} com {int} pessoa(s) na minha frente")
    public void estouNaPosicao(int posicao, int pessoasAFrente) throws Exception {
        JsonNode senha = corpo(resposta).get(0);
        assertThat(senha.get("posicao").asInt()).isEqualTo(posicao);
        assertThat(senha.get("pessoasAFrente").asInt()).isEqualTo(pessoasAFrente);
    }

    @Entao("nenhuma senha aparece para mim")
    public void nenhumaSenhaAparece() throws Exception {
        assertThat(resposta.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resposta)).isEmpty();
    }

    @E("vejo a opção de entrar na fila do serviço {string}")
    public void vejoAOpcaoDeEntrarNaFila(String nomeDoServico) throws Exception {
        // A tela mostra "Entrar na fila" para cada serviço desta lista (SCRUM-140).
        MvcResult servicos = consultar("/api/paciente/servicos");
        assertThat(servicos.getResponse().getStatus()).isEqualTo(200);

        JsonNode encontrado = null;
        for (JsonNode item : corpo(servicos)) {
            if (item.get("nome").asText().equals(nomeDoServico)) {
                encontrado = item;
            }
        }
        assertThat(encontrado).as("o serviço %s deve aparecer na lista", nomeDoServico).isNotNull();
        assertThat(encontrado.get("aberta").asBoolean()).isTrue();
    }

    private void criarPacienteEServico(String nomeDoServico) {
        usuario = usuarioRepository.save(new Usuario("Paciente Fictício", "paciente.posicao.bdd@exemplo.test",
            protecaoCpf.hash(CPF), protecaoCpf.cifrar(CPF), "hash", Perfil.PACIENTE));
        Paciente novoPaciente = new Paciente("Paciente Fictício", protecaoCpf.hash(CPF), protecaoCpf.cifrar(CPF),
            Prioridade.NENHUMA, Instant.now(clock));
        novoPaciente.setUsuario(usuario);
        paciente = pacienteRepository.save(novoPaciente);

        servico = servicoRepository.save(new Servico(nomeDoServico));
        configuracaoServicoRepository.save(new ConfiguracaoServico(servico,
            LocalTime.of(7, 0), LocalTime.of(17, 0), 40));
    }

    private Senha novaSenha(Paciente dono, int numero, StatusSenha status) {
        Senha senha = new Senha(servico, dono, numero, LocalDate.now(clock), Prioridade.NENHUMA, Instant.now(clock));
        senha.setStatus(status);
        return senha;
    }

    private MvcResult consultar(String url) throws Exception {
        return mockMvc.perform(get(url)
                .header("Authorization", "Bearer " + jwtService.gerarToken(usuario)))
            .andReturn();
    }

    private JsonNode corpo(MvcResult resultado) throws Exception {
        // Sem charset no Content-Type o MockMvc leria em ISO-8859-1 e estragaria os acentos.
        return objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
