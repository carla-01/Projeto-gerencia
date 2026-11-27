package br.uepb.filasus.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

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

/** Passos da US08 - Cancelar minha senha (SCRUM-43). Dados fictícios. */
public class CancelarSenhaSteps {

    /** CPFs fictícios, só para o hash e a cifra; não são validados aqui. */
    private static final String CPF = "86288366757";
    private static final String[] CPFS_DOS_OUTROS = {"45317828791", "15350946056"};

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
    private Paciente[] outrosPacientes = new Paciente[CPFS_DOS_OUTROS.length];
    private Servico servico;
    private Senha minhaSenha;
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
        for (Paciente outro : outrosPacientes) {
            if (outro != null) {
                pacienteRepository.delete(outro);
            }
        }
        pacienteRepository.delete(paciente);
        usuarioRepository.delete(usuario);
    }

    @Dado("que estou na fila do serviço {string} com a senha número {int}")
    public void queEstouNaFila(String nomeDoServico, int numero) {
        usuario = usuarioRepository.save(new Usuario("Paciente Fictício", "paciente.cancelar.bdd@exemplo.test",
            protecaoCpf.hash(CPF), protecaoCpf.cifrar(CPF), "hash", Perfil.PACIENTE));
        Paciente novoPaciente = new Paciente("Paciente Fictício", protecaoCpf.hash(CPF), protecaoCpf.cifrar(CPF),
            Prioridade.NENHUMA, Instant.now(clock));
        novoPaciente.setUsuario(usuario);
        paciente = pacienteRepository.save(novoPaciente);

        servico = servicoRepository.save(new Servico(nomeDoServico));
        configuracaoServicoRepository.save(new ConfiguracaoServico(servico,
            LocalTime.of(7, 0), LocalTime.of(17, 0), 40));

        minhaSenha = senhaRepository.save(novaSenha(paciente, numero));
    }

    @E("há mais {int} pacientes aguardando depois de mim")
    public void haMaisPacientesAguardando(int quantidade) {
        for (int i = 0; i < quantidade; i++) {
            String cpf = CPFS_DOS_OUTROS[i];
            // Paciente cadastrado pela recepção, sem login próprio.
            Paciente outro = new Paciente("Outro Paciente " + (i + 1), protecaoCpf.hash(cpf),
                protecaoCpf.cifrar(cpf), Prioridade.NENHUMA, Instant.now(clock));
            outrosPacientes[i] = pacienteRepository.save(outro);
            senhaRepository.save(novaSenha(outrosPacientes[i], minhaSenha.getNumero() + i + 1));
        }
    }

    @E("o meu atendimento já começou")
    public void oMeuAtendimentoJaComecou() {
        minhaSenha.setStatus(StatusSenha.EM_ATENDIMENTO);
        minhaSenha = senhaRepository.save(minhaSenha);
    }

    @Quando("cancelo a minha senha")
    public void canceloAMinhaSenha() throws Exception {
        resposta = mockMvc.perform(post("/api/paciente/senhas/" + minhaSenha.getId() + "/cancelar")
                .header("Authorization", "Bearer " + jwtService.gerarToken(usuario)))
            .andReturn();
    }

    @Entao("a minha senha fica com o status {string}")
    public void aMinhaSenhaFicaComOStatus(String status) {
        Senha noBanco = senhaRepository.findById(minhaSenha.getId()).get();
        assertEquals(status, noBanco.getStatus().name());
    }

    @E("restam {int} senhas na fila do serviço")
    public void restamSenhasNaFila(int quantidade) {
        int naFila = 0;
        for (Senha senha : senhaRepository.findAll()) {
            boolean contaNaFila = senha.getStatus() == StatusSenha.AGUARDANDO
                || senha.getStatus() == StatusSenha.PRESENTE;
            if (contaNaFila) {
                naFila = naFila + 1;
            }
        }
        assertEquals(quantidade, naFila);
    }

    @Entao("recebo a mensagem {string}")
    public void receboAMensagem(String mensagem) throws Exception {
        assertEquals(409, resposta.getResponse().getStatus());
        // Sem charset no Content-Type o MockMvc leria em ISO-8859-1 e estragaria os acentos.
        JsonNode corpo = objectMapper.readTree(resposta.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals(mensagem, corpo.get("mensagem").asText());
    }

    private Senha novaSenha(Paciente dono, int numero) {
        return new Senha(servico, dono, numero, LocalDate.now(clock), Prioridade.NENHUMA, Instant.now(clock));
    }
}
