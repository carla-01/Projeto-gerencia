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

/** O paciente chamado continua com a senha ativa (US-13, Cenário 4 - SCRUM-163). Dados fictícios. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SenhaChamadaAtivaTest {

    /** 12:00 UTC = 09:00 no horário da UBS. */
    private static final Instant AGORA = Instant.parse("2026-11-25T12:00:00Z");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 25);

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
    void criarDados() {
        limparFila();
        usuario = usuarioRepository.save(new Usuario("Paciente Fictício", "chamado.ativa@exemplo.test",
            protecaoCpf.hash("cpf-paciente-chamado"), protecaoCpf.cifrar("52998224725"), "hash", Perfil.PACIENTE));
        Paciente novo = new Paciente("Paciente Fictício", protecaoCpf.hash("cpf-paciente-chamado"),
            protecaoCpf.cifrar("52998224725"), Prioridade.NENHUMA, AGORA);
        novo.setUsuario(usuario);
        paciente = pacienteRepository.save(novo);
        clinico = servicoRepository.save(new Servico("Clínico"));
        configuracaoServicoRepository.save(new ConfiguracaoServico(clinico, LocalTime.of(7, 0),
            LocalTime.of(17, 0), 40));
    }

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

    private void criarSenha(StatusSenha situacao) {
        Senha senha = new Senha(clinico, paciente, 1, HOJE, Prioridade.NENHUMA, AGORA);
        senha.setStatus(situacao);
        senhaRepository.save(senha);
    }

    private ResultActions entrarNaFila() throws Exception {
        return mockMvc.perform(post("/api/paciente/senhas")
            .header("Authorization", "Bearer " + jwtService.gerarToken(usuario))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"servicoId\":" + clinico.getId() + "}"));
    }

    private ResultActions verMinhaFila() throws Exception {
        return mockMvc.perform(get("/api/paciente/senhas")
            .header("Authorization", "Bearer " + jwtService.gerarToken(usuario)));
    }

    @Test
    void pacienteChamadoNaoTiraOutraSenha() throws Exception {
        criarSenha(StatusSenha.CHAMADA);

        entrarNaFila().andExpect(status().isConflict());

        assertEquals(1, senhaRepository.count());
    }

    @Test
    void pacienteEmAtendimentoNaoTiraOutraSenha() throws Exception {
        criarSenha(StatusSenha.EM_ATENDIMENTO);

        entrarNaFila().andExpect(status().isConflict());

        assertEquals(1, senhaRepository.count());
    }

    @Test
    void depoisDeAtendidoPodeTirarOutraSenha() throws Exception {
        criarSenha(StatusSenha.ATENDIDA);

        entrarNaFila().andExpect(status().isCreated());

        assertEquals(2, senhaRepository.count());
    }

    @Test
    void senhaChamadaContinuaAparecendoSemPosicao() throws Exception {
        criarSenha(StatusSenha.CHAMADA);

        verMinhaFila()
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("CHAMADA"))
            .andExpect(jsonPath("$[0].posicao").value(0))
            .andExpect(jsonPath("$[0].pessoasAFrente").value(0));
    }

    @Test
    void senhaChamadaNaoContaNaFilaDoServico() {
        criarSenha(StatusSenha.CHAMADA);

        assertEquals(0, senhaRepository.filaDoDia(clinico, HOJE).size());
    }
}
