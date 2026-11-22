package br.uepb.filasus.fila;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

/** Prioridade legal marcada pela recepção (US-11 - SCRUM-158). Dados fictícios. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrioridadeSenhaControllerTest {

    /** 12:00 UTC = 09:00 no horário da UBS. */
    private static final Instant AGORA = Instant.parse("2026-11-24T12:00:00Z");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 24);
    private static final String CPF_PACIENTE = "52998224725";
    private static final String CPF_RECEPCIONISTA = "11144477735";

    /** Relógio parado, para conferir o "quando" da prioridade. */
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
    private SenhaRepository senhaRepository;

    private Usuario recepcionista;
    private Usuario usuarioPaciente;
    private Paciente paciente;
    private Servico clinico;

    @BeforeEach
    void criarDados() {
        limparFila();
        recepcionista = usuarioRepository.save(new Usuario("Recepcionista Fictícia", "recepcao.prioridade@exemplo.test",
            protecaoCpf.hash(CPF_RECEPCIONISTA), protecaoCpf.cifrar(CPF_RECEPCIONISTA), "hash", Perfil.RECEPCIONISTA));
        usuarioPaciente = usuarioRepository.save(new Usuario("Paciente Fictício", "paciente.prioridade@exemplo.test",
            protecaoCpf.hash(CPF_PACIENTE), protecaoCpf.cifrar(CPF_PACIENTE), "hash", Perfil.PACIENTE));

        Paciente novo = new Paciente("Paciente Fictício", protecaoCpf.hash(CPF_PACIENTE),
            protecaoCpf.cifrar(CPF_PACIENTE), Prioridade.NENHUMA, AGORA);
        novo.setUsuario(usuarioPaciente);
        paciente = pacienteRepository.save(novo);

        clinico = servicoRepository.save(new Servico("Clínico"));
    }

    /** Apaga só o que o teste criou, para não atrapalhar a limpeza dos outros testes. */
    @AfterEach
    void limparDados() {
        limparFila();
        pacienteRepository.delete(paciente);
        usuarioRepository.delete(usuarioPaciente);
        usuarioRepository.delete(recepcionista);
    }

    private void limparFila() {
        senhaRepository.deleteAll();
        servicoRepository.deleteAll();
    }

    private Senha senhaDeHoje(StatusSenha status) {
        Senha senha = new Senha(clinico, paciente, 1, HOJE, Prioridade.NENHUMA, AGORA);
        senha.setStatus(status);
        return senhaRepository.save(senha);
    }

    private ResultActions marcar(Usuario quemMarca, Long senhaId, String corpo) throws Exception {
        return mockMvc.perform(put("/api/recepcao/senhas/" + senhaId + "/prioridade")
            .header("Authorization", "Bearer " + jwtService.gerarToken(quemMarca))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    private Senha noBanco(Senha senha) {
        return senhaRepository.findById(senha.getId()).get();
    }

    @Test
    void cenario1RecepcaoMarcaIdosoEAMudancaFicaRegistrada() throws Exception {
        Senha senha = senhaDeHoje(StatusSenha.AGUARDANDO);

        marcar(recepcionista, senha.getId(), "{\"prioridade\":\"IDOSO\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numero").value(1))
            .andExpect(jsonPath("$.prioridade").value("IDOSO"))
            .andExpect(jsonPath("$.prioridadePor").value(recepcionista.getId()))
            .andExpect(jsonPath("$.prioridadeEm").value("2026-11-24T12:00:00Z"));

        Senha salva = noBanco(senha);
        assertEquals(Prioridade.IDOSO, salva.getPrioridadeAplicada());
        assertEquals(recepcionista.getId(), salva.getPrioridadePor().getId());
        assertEquals(AGORA, salva.getPrioridadeEm());
        assertNull(salva.getPrioridadeMotivo());
    }

    @Test
    void marcaPrioridadeDepoisDoCheckin() throws Exception {
        Senha senha = senhaDeHoje(StatusSenha.PRESENTE);

        marcar(recepcionista, senha.getId(), "{\"prioridade\":\"GESTANTE\"}").andExpect(status().isOk());

        assertEquals(Prioridade.GESTANTE, noBanco(senha).getPrioridadeAplicada());
    }

    @Test
    void urgenciaComMotivoGuardaOMotivo() throws Exception {
        Senha senha = senhaDeHoje(StatusSenha.AGUARDANDO);

        marcar(recepcionista, senha.getId(), "{\"prioridade\":\"URGENCIA\",\"motivo\":\"  Falta de ar  \"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.prioridade").value("URGENCIA"));

        Senha salva = noBanco(senha);
        assertEquals(Prioridade.URGENCIA, salva.getPrioridadeAplicada());
        assertEquals("Falta de ar", salva.getPrioridadeMotivo());
    }

    @Test
    void cenario2UrgenciaSemMotivoNaoSalva() throws Exception {
        Senha senha = senhaDeHoje(StatusSenha.AGUARDANDO);

        marcar(recepcionista, senha.getId(), "{\"prioridade\":\"URGENCIA\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("motivo"))
            .andExpect(jsonPath("$.mensagem").value("Informe o motivo da urgência"));

        Senha salva = noBanco(senha);
        assertEquals(Prioridade.NENHUMA, salva.getPrioridadeAplicada());
        assertNull(salva.getPrioridadeEm());
    }

    @Test
    void urgenciaComMotivoEmBrancoNaoSalva() throws Exception {
        Senha senha = senhaDeHoje(StatusSenha.AGUARDANDO);

        marcar(recepcionista, senha.getId(), "{\"prioridade\":\"URGENCIA\",\"motivo\":\"   \"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem").value("Informe o motivo da urgência"));

        assertEquals(Prioridade.NENHUMA, noBanco(senha).getPrioridadeAplicada());
    }

    @Test
    void motivoMuitoLongoNaoSalva() throws Exception {
        Senha senha = senhaDeHoje(StatusSenha.AGUARDANDO);
        String motivo = "a".repeat(256);

        marcar(recepcionista, senha.getId(), "{\"prioridade\":\"URGENCIA\",\"motivo\":\"" + motivo + "\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("motivo"));

        assertEquals(Prioridade.NENHUMA, noBanco(senha).getPrioridadeAplicada());
    }

    @Test
    void semPrioridadeNaoSalva() throws Exception {
        Senha senha = senhaDeHoje(StatusSenha.AGUARDANDO);

        marcar(recepcionista, senha.getId(), "{}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("prioridade"))
            .andExpect(jsonPath("$.mensagem").value("Escolha a prioridade"));
    }

    @Test
    void nenhumaNaoEUmaMarcacao() throws Exception {
        Senha senha = senhaDeHoje(StatusSenha.AGUARDANDO);

        marcar(recepcionista, senha.getId(), "{\"prioridade\":\"NENHUMA\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("prioridade"))
            .andExpect(jsonPath("$.mensagem").value("Escolha IDOSO, GESTANTE, PCD ou URGENCIA"));

        assertNull(noBanco(senha).getPrioridadeEm());
    }

    @Test
    void prioridadeQueNaoExisteNaoSalva() throws Exception {
        Senha senha = senhaDeHoje(StatusSenha.AGUARDANDO);

        marcar(recepcionista, senha.getId(), "{\"prioridade\":\"VIP\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("prioridade"));

        assertEquals(Prioridade.NENHUMA, noBanco(senha).getPrioridadeAplicada());
    }

    @Test
    void senhaQueNaoExisteResponde404() throws Exception {
        marcar(recepcionista, 999_999L, "{\"prioridade\":\"IDOSO\"}")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensagem").value("Senha não encontrada"));
    }

    @Test
    void senhaJaChamadaNaoMudaDePrioridade() throws Exception {
        Senha senha = senhaDeHoje(StatusSenha.CHAMADA);

        marcar(recepcionista, senha.getId(), "{\"prioridade\":\"IDOSO\"}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Esta senha não está mais na fila"));

        assertEquals(Prioridade.NENHUMA, noBanco(senha).getPrioridadeAplicada());
    }

    @Test
    void senhaCanceladaNaoMudaDePrioridade() throws Exception {
        Senha senha = senhaDeHoje(StatusSenha.CANCELADA);

        marcar(recepcionista, senha.getId(), "{\"prioridade\":\"PCD\"}").andExpect(status().isConflict());

        assertEquals(Prioridade.NENHUMA, noBanco(senha).getPrioridadeAplicada());
    }

    @Test
    void pacienteNaoMarcaPrioridade() throws Exception {
        Senha senha = senhaDeHoje(StatusSenha.AGUARDANDO);

        marcar(usuarioPaciente, senha.getId(), "{\"prioridade\":\"IDOSO\"}").andExpect(status().isForbidden());

        assertEquals(Prioridade.NENHUMA, noBanco(senha).getPrioridadeAplicada());
    }

    @Test
    void semLoginResponde401() throws Exception {
        Senha senha = senhaDeHoje(StatusSenha.AGUARDANDO);

        mockMvc.perform(put("/api/recepcao/senhas/" + senha.getId() + "/prioridade")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prioridade\":\"IDOSO\"}"))
            .andExpect(status().isUnauthorized());
    }
}
