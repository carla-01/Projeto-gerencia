package br.uepb.filasus.fila;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** Posição do paciente na fila (US-07 - SCRUM-139). Dados fictícios. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PosicaoFilaControllerTest {

    /** 12:00 UTC = 09:00 no horário da UBS. */
    private static final Instant AGORA = Instant.parse("2026-11-10T12:00:00Z");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);
    private static final String CPF = "12345678909";
    private static final String CPF_OUTRO = "98765432100";

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
    private Paciente outroPaciente;
    private Servico clinico;
    private Servico odontologia;

    @BeforeEach
    void criarPacientesEServicos() {
        limparFila();
        usuario = usuarioRepository.save(new Usuario("Paciente Fictício", "paciente.posicao@exemplo.test",
            protecaoCpf.hash(CPF), protecaoCpf.cifrar(CPF), "hash", Perfil.PACIENTE));
        paciente = new Paciente("Paciente Fictício", protecaoCpf.hash(CPF), protecaoCpf.cifrar(CPF),
            Prioridade.NENHUMA, AGORA);
        paciente.setUsuario(usuario);
        paciente = pacienteRepository.save(paciente);

        // Paciente cadastrado pela recepção, sem login próprio; só ocupa lugar na fila.
        outroPaciente = pacienteRepository.save(new Paciente("Outro Paciente", protecaoCpf.hash(CPF_OUTRO),
            protecaoCpf.cifrar(CPF_OUTRO), Prioridade.NENHUMA, AGORA));

        clinico = servicoComHorario("Clínico");
        odontologia = servicoComHorario("Odontologia");
    }

    /** Apaga só o que o teste criou, para não atrapalhar a limpeza dos outros testes. */
    @AfterEach
    void limparDados() {
        limparFila();
        pacienteRepository.delete(paciente);
        pacienteRepository.delete(outroPaciente);
        usuarioRepository.delete(usuario);
    }

    private void limparFila() {
        senhaRepository.deleteAll();
        configuracaoServicoRepository.deleteAll();
        servicoRepository.deleteAll();
    }

    private Servico servicoComHorario(String nome) {
        Servico servico = servicoRepository.save(new Servico(nome));
        configuracaoServicoRepository.save(new ConfiguracaoServico(servico,
            LocalTime.of(7, 0), LocalTime.of(17, 0), 40));
        return servico;
    }

    private Senha senha(Servico servico, Paciente dono, int numero, LocalDate dia, StatusSenha status) {
        Senha senha = new Senha(servico, dono, numero, dia, Prioridade.NENHUMA, AGORA);
        senha.setStatus(status);
        return senhaRepository.save(senha);
    }

    private ResultActions minhasSenhas(Usuario quem) throws Exception {
        return mockMvc.perform(get("/api/paciente/senhas")
            .header("Authorization", "Bearer " + jwtService.gerarToken(quem)));
    }

    @Test
    void pacienteVeASenhaAPosicaoEQuantasPessoasEstaoNaFrente() throws Exception {
        senha(clinico, outroPaciente, 1, HOJE, StatusSenha.AGUARDANDO);
        senha(clinico, outroPaciente, 2, HOJE, StatusSenha.PRESENTE);
        senha(clinico, paciente, 3, HOJE, StatusSenha.AGUARDANDO);

        minhasSenhas(usuario)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].numero").value(3))
            .andExpect(jsonPath("$[0].servico").value("Clínico"))
            .andExpect(jsonPath("$[0].data").value("2026-11-10"))
            .andExpect(jsonPath("$[0].status").value("AGUARDANDO"))
            .andExpect(jsonPath("$[0].posicao").value(3))
            .andExpect(jsonPath("$[0].pessoasAFrente").value(2));
    }

    @Test
    void primeiroDaFilaNaoTemNinguemNaFrente() throws Exception {
        senha(clinico, paciente, 1, HOJE, StatusSenha.AGUARDANDO);
        senha(clinico, outroPaciente, 2, HOJE, StatusSenha.AGUARDANDO);

        minhasSenhas(usuario)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].posicao").value(1))
            .andExpect(jsonPath("$[0].pessoasAFrente").value(0));
    }

    @Test
    void senhasQueJaSairamDaFilaNaoContamNaFrente() throws Exception {
        senha(clinico, outroPaciente, 1, HOJE, StatusSenha.ATENDIDA);
        senha(clinico, outroPaciente, 2, HOJE, StatusSenha.CANCELADA);
        senha(clinico, outroPaciente, 3, HOJE, StatusSenha.EM_ATENDIMENTO);
        senha(clinico, outroPaciente, 4, HOJE, StatusSenha.CHAMADA);
        senha(clinico, outroPaciente, 5, HOJE, StatusSenha.AGUARDANDO);
        senha(clinico, paciente, 6, HOJE, StatusSenha.AGUARDANDO);

        minhasSenhas(usuario)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].posicao").value(2))
            .andExpect(jsonPath("$[0].pessoasAFrente").value(1));
    }

    @Test
    void quemEstaAtrasNaoContaNaFrente() throws Exception {
        senha(clinico, paciente, 1, HOJE, StatusSenha.AGUARDANDO);
        senha(clinico, outroPaciente, 2, HOJE, StatusSenha.AGUARDANDO);
        senha(clinico, outroPaciente, 3, HOJE, StatusSenha.AGUARDANDO);

        minhasSenhas(usuario)
            .andExpect(jsonPath("$[0].pessoasAFrente").value(0));
    }

    @Test
    void cadaServicoTemASuaPropriaFila() throws Exception {
        senha(clinico, outroPaciente, 1, HOJE, StatusSenha.AGUARDANDO);
        senha(clinico, paciente, 2, HOJE, StatusSenha.AGUARDANDO);
        senha(odontologia, paciente, 1, HOJE, StatusSenha.AGUARDANDO);

        minhasSenhas(usuario)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.servico == 'Clínico')].pessoasAFrente").value(1))
            .andExpect(jsonPath("$[?(@.servico == 'Odontologia')].pessoasAFrente").value(0));
    }

    @Test
    void senhaDeOntemNaoAparece() throws Exception {
        senha(clinico, paciente, 1, HOJE.minusDays(1), StatusSenha.AGUARDANDO);

        minhasSenhas(usuario)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void pacienteSemSenhaAtivaRecebeListaVazia() throws Exception {
        senha(clinico, paciente, 1, HOJE, StatusSenha.ATENDIDA);
        senha(odontologia, paciente, 1, HOJE, StatusSenha.CANCELADA);

        minhasSenhas(usuario)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void outroPerfilRecebe403() throws Exception {
        Usuario recepcionista = new Usuario("Recepção Fictícia", "recepcao.posicao@exemplo.test",
            "cpf-hash", "cpf-cifrado", "hash", Perfil.RECEPCIONISTA);

        minhasSenhas(recepcionista).andExpect(status().isForbidden());
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mockMvc.perform(get("/api/paciente/senhas"))
            .andExpect(status().isUnauthorized());
    }
}
