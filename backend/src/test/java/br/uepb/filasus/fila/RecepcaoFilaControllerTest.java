package br.uepb.filasus.fila;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.uepb.filasus.auth.JwtService;
import br.uepb.filasus.lgpd.ProtecaoCpf;
import br.uepb.filasus.paciente.Paciente;
import br.uepb.filasus.paciente.PacienteRepository;
import br.uepb.filasus.paciente.Prioridade;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;

/** A recepção coloca o paciente na fila (US-10 - SCRUM-155). Dados fictícios. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecepcaoFilaControllerTest {

    /** 12:00 UTC = 09:00 no horário da UBS. */
    private static final Instant AGORA = Instant.parse("2026-11-24T12:00:00Z");
    private static final String CPF_PACIENTE = "52998224725";
    private static final String CPF_OUTRO_PACIENTE = "39053344705";
    private static final String CPF_RECEPCIONISTA = "11144477735";
    private static final String CPF_GESTOR = "12345678909";

    @TestConfiguration
    static class RelogioDeTesteConfig {

        @Bean
        @Primary
        Clock relogioDeTeste() {
            return Clock.fixed(AGORA, ZoneId.of("America/Fortaleza"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ProtecaoCpf protecaoCpf;

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

    private Usuario recepcionista;
    private Usuario gestor;
    private Paciente paciente;
    private Paciente outroPaciente;
    private Servico clinico;

    @BeforeEach
    void criarDados() {
        limparFila();
        recepcionista = criarUsuario("Recepcionista Fictícia", "recepcao.fila@exemplo.test", CPF_RECEPCIONISTA,
            Perfil.RECEPCIONISTA);
        gestor = criarUsuario("Gestor Fictício", "gestor.fila@exemplo.test", CPF_GESTOR, Perfil.GESTOR);
        // Paciente cadastrado no balcão: não tem login.
        paciente = criarPaciente("Paciente Sem Login", CPF_PACIENTE);
        outroPaciente = criarPaciente("Outro Paciente", CPF_OUTRO_PACIENTE);
        clinico = servicoRepository.save(new Servico("Clínico"));
    }

    /** Apaga só o que o teste criou, para não atrapalhar a limpeza dos outros testes. */
    @AfterEach
    void limparDados() {
        limparFila();
        pacienteRepository.delete(paciente);
        pacienteRepository.delete(outroPaciente);
        usuarioRepository.delete(gestor);
        usuarioRepository.delete(recepcionista);
    }

    private void limparFila() {
        senhaRepository.deleteAll();
        configuracaoServicoRepository.deleteAll();
        servicoRepository.deleteAll();
    }

    private Usuario criarUsuario(String nome, String email, String cpf, Perfil perfil) {
        return usuarioRepository.save(new Usuario(nome, email, protecaoCpf.hash(cpf), protecaoCpf.cifrar(cpf),
            "hash", perfil));
    }

    private Paciente criarPaciente(String nome, String cpf) {
        return pacienteRepository.save(new Paciente(nome, protecaoCpf.hash(cpf), protecaoCpf.cifrar(cpf),
            Prioridade.NENHUMA, AGORA));
    }

    /** Abre a fila do Clínico: das 07:00 até o horário de fechamento, com o limite de senhas. */
    private void configurarFila(int horaFechamento, int limite) {
        configuracaoServicoRepository.save(new ConfiguracaoServico(clinico, LocalTime.of(7, 0),
            LocalTime.of(horaFechamento, 0), limite));
    }

    private ResultActions colocarNaFila(Usuario quem, String corpo) throws Exception {
        return mockMvc.perform(post("/api/recepcao/senhas")
            .header("Authorization", "Bearer " + jwtService.gerarToken(quem))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    private String corpo(Long pacienteId, Long servicoId) {
        return "{\"pacienteId\":" + pacienteId + ",\"servicoId\":" + servicoId + "}";
    }

    @Test
    void listaOsServicosDizendoSeAFilaEstaAberta() throws Exception {
        configurarFila(17, 40);

        mockMvc.perform(get("/api/recepcao/servicos")
                .header("Authorization", "Bearer " + jwtService.gerarToken(recepcionista)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nome").value("Clínico"))
            .andExpect(jsonPath("$[0].aberta").value(true));
    }

    @Test
    void colocaNaFilaOPacienteSemLogin() throws Exception {
        configurarFila(17, 40);

        colocarNaFila(recepcionista, corpo(paciente.getId(), clinico.getId()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numero").value(1))
            .andExpect(jsonPath("$.servico").value("Clínico"))
            .andExpect(jsonPath("$.status").value("AGUARDANDO"));

        assertEquals(1, senhaRepository.count());
    }

    @Test
    void naoCriaSegundaSenhaNoMesmoServico() throws Exception {
        configurarFila(17, 40);
        colocarNaFila(recepcionista, corpo(paciente.getId(), clinico.getId()));

        colocarNaFila(recepcionista, corpo(paciente.getId(), clinico.getId()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Este paciente já tem uma senha ativa neste serviço"));

        assertEquals(1, senhaRepository.count());
    }

    @Test
    void respeitaOHorarioDoServico() throws Exception {
        // A fila fechou às 08:00 e o relógio do teste marca 09:00.
        configurarFila(8, 40);

        colocarNaFila(recepcionista, corpo(paciente.getId(), clinico.getId()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Fila fechada"));

        assertEquals(0, senhaRepository.count());
    }

    @Test
    void respeitaOLimiteDeSenhasDoDia() throws Exception {
        configurarFila(17, 1);
        colocarNaFila(recepcionista, corpo(outroPaciente.getId(), clinico.getId()));

        colocarNaFila(recepcionista, corpo(paciente.getId(), clinico.getId()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Fila fechada"));

        assertEquals(1, senhaRepository.count());
    }

    @Test
    void pacienteQueNaoExisteResponde404() throws Exception {
        configurarFila(17, 40);

        colocarNaFila(recepcionista, corpo(999_999L, clinico.getId())).andExpect(status().isNotFound());
    }

    @Test
    void servicoQueNaoExisteResponde404() throws Exception {
        colocarNaFila(recepcionista, corpo(paciente.getId(), 999_999L)).andExpect(status().isNotFound());
    }

    @Test
    void semServicoPedeParaEscolher() throws Exception {
        colocarNaFila(recepcionista, "{\"pacienteId\":" + paciente.getId() + "}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem").value("Escolha o serviço"));
    }

    @Test
    void outroPerfilNaoColocaNaFila() throws Exception {
        configurarFila(17, 40);

        colocarNaFila(gestor, corpo(paciente.getId(), clinico.getId())).andExpect(status().isForbidden());

        assertEquals(0, senhaRepository.count());
    }
}
