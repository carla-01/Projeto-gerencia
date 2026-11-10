package br.uepb.filasus.fila;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

/** Entrada na fila pelo paciente (US-06 - SCRUM-136). Dados fictícios. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EntradaFilaControllerTest {

    /** 12:00 UTC = 09:00 no horário da UBS, dentro do horário de 07:00 às 17:00. */
    private static final Instant AGORA = Instant.parse("2026-11-10T12:00:00Z");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);
    private static final String CPF = "39053344705";

    /** Relógio parado, para o teste não depender da hora em que o CI roda. */
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

    private Usuario usuario;
    private Paciente paciente;
    private Servico clinico;

    @BeforeEach
    void criarPacienteEServico() {
        limparFila();
        usuario = usuarioRepository.save(new Usuario("Paciente Fictício", "paciente.fila@exemplo.test",
            protecaoCpf.hash(CPF), protecaoCpf.cifrar(CPF), "hash", Perfil.PACIENTE));
        paciente = new Paciente("Paciente Fictício", protecaoCpf.hash(CPF), protecaoCpf.cifrar(CPF),
            Prioridade.NENHUMA, AGORA);
        paciente.setUsuario(usuario);
        paciente = pacienteRepository.save(paciente);
        clinico = servicoComHorario("Clínico", 7, 17, 40);
    }

    /** Apaga só o que o teste criou, para não atrapalhar a limpeza dos outros testes. */
    @AfterEach
    void limparDados() {
        limparFila();
        pacienteRepository.delete(paciente);
        usuarioRepository.delete(usuario);
    }

    private void limparFila() {
        senhaRepository.deleteAll();
        configuracaoServicoRepository.deleteAll();
        servicoRepository.deleteAll();
    }

    private Servico servicoComHorario(String nome, int abertura, int fechamento, int limite) {
        Servico servico = servicoRepository.save(new Servico(nome));
        configuracaoServicoRepository.save(new ConfiguracaoServico(servico,
            LocalTime.of(abertura, 0), LocalTime.of(fechamento, 0), limite));
        return servico;
    }

    private Senha senhaDeHoje(Servico servico, int numero, StatusSenha status) {
        Senha senha = new Senha(servico, paciente, numero, HOJE, Prioridade.NENHUMA, AGORA);
        senha.setStatus(status);
        return senhaRepository.save(senha);
    }

    private ResultActions entrarNaFila(Long servicoId) throws Exception {
        return mockMvc.perform(post("/api/paciente/senhas")
            .header("Authorization", "Bearer " + jwtService.gerarToken(usuario))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"servicoId\": " + servicoId + "}"));
    }

    @Test
    void pacienteEntraNaFilaERecebeASenha1() throws Exception {
        entrarNaFila(clinico.getId())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numero").value(1))
            .andExpect(jsonPath("$.servico").value("Clínico"))
            .andExpect(jsonPath("$.data").value("2026-11-10"))
            .andExpect(jsonPath("$.status").value("AGUARDANDO"));

        assertEquals(1, senhaRepository.count());
    }

    @Test
    void recebeOProximoNumeroDoDia() throws Exception {
        senhaDeHoje(clinico, 5, StatusSenha.CANCELADA);

        entrarNaFila(clinico.getId())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numero").value(6));
    }

    @Test
    void naoTiraDuasSenhasAtivasNoMesmoServico() throws Exception {
        entrarNaFila(clinico.getId()).andExpect(status().isCreated());

        entrarNaFila(clinico.getId())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Você já tem uma senha ativa neste serviço"));

        assertEquals(1, senhaRepository.count());
    }

    @Test
    void podeEntrarEmOutroServicoNoMesmoDia() throws Exception {
        Servico enfermagem = servicoComHorario("Enfermagem", 7, 17, 60);
        entrarNaFila(clinico.getId()).andExpect(status().isCreated());

        entrarNaFila(enfermagem.getId())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numero").value(1));
    }

    @Test
    void filaFechadaForaDoHorario() throws Exception {
        Servico tarde = servicoComHorario("Odontologia", 13, 17, 20);

        entrarNaFila(tarde.getId())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Fila fechada"));

        assertEquals(0, senhaRepository.count());
    }

    @Test
    void filaFechadaQuandoOLimiteDoDiaAcabou() throws Exception {
        Servico lotado = servicoComHorario("Odontologia", 7, 17, 1);
        senhaDeHoje(lotado, 1, StatusSenha.ATENDIDA);

        entrarNaFila(lotado.getId())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Fila fechada"));
    }

    @Test
    void servicoDesativadoNaoRecebeSenha() throws Exception {
        clinico.setAtivo(false);
        servicoRepository.save(clinico);

        entrarNaFila(clinico.getId())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensagem").value("Serviço não encontrado"));
    }

    @Test
    void servicoSemConfiguracaoFicaFechado() throws Exception {
        Servico semHorario = servicoRepository.save(new Servico("Odontologia"));

        entrarNaFila(semHorario.getId())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Fila fechada"));
    }

    @Test
    void servicoObrigatorio() throws Exception {
        mockMvc.perform(post("/api/paciente/senhas")
                .header("Authorization", "Bearer " + jwtService.gerarToken(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("servicoId"))
            .andExpect(jsonPath("$.mensagem").value("Serviço é obrigatório"));
    }

    @Test
    void outroPerfilRecebe403() throws Exception {
        Usuario recepcionista = new Usuario("Recepção Fictícia", "recepcao@exemplo.test",
            "cpf-hash", "cpf-cifrado", "hash", Perfil.RECEPCIONISTA);

        mockMvc.perform(post("/api/paciente/senhas")
                .header("Authorization", "Bearer " + jwtService.gerarToken(recepcionista))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"servicoId\": " + clinico.getId() + "}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mockMvc.perform(post("/api/paciente/senhas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"servicoId\": " + clinico.getId() + "}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listaOsServicosAtivosDizendoSeAFilaEstaAberta() throws Exception {
        servicoComHorario("Odontologia", 13, 17, 20);
        Servico desativado = servicoComHorario("Enfermagem", 7, 17, 60);
        desativado.setAtivo(false);
        servicoRepository.save(desativado);

        mockMvc.perform(get("/api/paciente/servicos")
                .header("Authorization", "Bearer " + jwtService.gerarToken(usuario)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].nome").value("Clínico"))
            .andExpect(jsonPath("$[0].aberta").value(true))
            .andExpect(jsonPath("$[1].nome").value("Odontologia"))
            .andExpect(jsonPath("$[1].aberta").value(false));
    }
}
