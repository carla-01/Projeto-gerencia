package br.uepb.filasus.fila;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** Check-in na recepção (US-10 - SCRUM-154). Dados fictícios. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckInControllerTest {

    /** 12:00 UTC = 09:00 no horário da UBS. */
    private static final Instant AGORA = Instant.parse("2026-11-24T12:00:00Z");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 24);
    private static final String CPF_PACIENTE = "52998224725";
    private static final String CPF_RECEPCIONISTA = "11144477735";
    private static final String CPF_SEM_CADASTRO = "39053344705";
    private static final String CPF_GESTOR = "12345678909";

    /** Relógio parado, para conferir o horário do check-in. */
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
    private Usuario outroPerfil;
    private Paciente paciente;
    private Servico clinico;

    @BeforeEach
    void criarDados() {
        limparFila();
        recepcionista = usuarioRepository.save(new Usuario("Recepcionista Fictícia", "recepcao.checkin@exemplo.test",
            protecaoCpf.hash(CPF_RECEPCIONISTA), protecaoCpf.cifrar(CPF_RECEPCIONISTA), "hash", Perfil.RECEPCIONISTA));
        outroPerfil = usuarioRepository.save(new Usuario("Gestor Fictício", "gestor.checkin@exemplo.test",
            protecaoCpf.hash(CPF_GESTOR), protecaoCpf.cifrar(CPF_GESTOR), "hash", Perfil.GESTOR));
        paciente = pacienteRepository.save(new Paciente("Paciente Fictício", protecaoCpf.hash(CPF_PACIENTE),
            protecaoCpf.cifrar(CPF_PACIENTE), Prioridade.NENHUMA, AGORA));
        clinico = servicoRepository.save(new Servico("Clínico"));
    }

    /** Apaga só o que o teste criou, para não atrapalhar a limpeza dos outros testes. */
    @AfterEach
    void limparDados() {
        limparFila();
        pacienteRepository.delete(paciente);
        usuarioRepository.delete(outroPerfil);
        usuarioRepository.delete(recepcionista);
    }

    private void limparFila() {
        senhaRepository.deleteAll();
        servicoRepository.deleteAll();
    }

    private Senha criarSenha(LocalDate dia, StatusSenha situacao) {
        Senha senha = new Senha(clinico, paciente, 7, dia, Prioridade.NENHUMA, AGORA);
        senha.setStatus(situacao);
        return senhaRepository.save(senha);
    }

    private ResultActions buscar(String busca) throws Exception {
        return mockMvc.perform(post("/api/recepcao/check-in/busca")
            .header("Authorization", "Bearer " + jwtService.gerarToken(recepcionista))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"busca\":\"" + busca + "\"}"));
    }

    private ResultActions confirmar(Usuario quem, Long senhaId) throws Exception {
        return mockMvc.perform(put("/api/recepcao/senhas/" + senhaId + "/check-in")
            .header("Authorization", "Bearer " + jwtService.gerarToken(quem)));
    }

    private Senha noBanco(Senha senha) {
        return senhaRepository.findById(senha.getId()).get();
    }

    @Test
    void buscaPeloCpfComPontosAchaASenhaDeHoje() throws Exception {
        criarSenha(HOJE, StatusSenha.AGUARDANDO);

        buscar("529.982.247-25")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.senhas[0].numero").value(7))
            .andExpect(jsonPath("$.senhas[0].servico").value("Clínico"))
            .andExpect(jsonPath("$.senhas[0].nomePaciente").value("Paciente Fictício"))
            .andExpect(jsonPath("$.senhas[0].cpfMascarado").value("529.***.***-25"))
            .andExpect(jsonPath("$.senhas[0].status").value("AGUARDANDO"));
    }

    @Test
    void buscaPeloNumeroAchaASenhaDeHoje() throws Exception {
        criarSenha(HOJE, StatusSenha.AGUARDANDO);

        buscar("7")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.senhas[0].nomePaciente").value("Paciente Fictício"));
    }

    @Test
    void cenario1ConfirmarPresencaMudaParaPresenteEGuardaOHorario() throws Exception {
        Senha senha = criarSenha(HOJE, StatusSenha.AGUARDANDO);

        confirmar(recepcionista, senha.getId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PRESENTE"))
            .andExpect(jsonPath("$.checkinEm").value("2026-11-24T12:00:00Z"));

        Senha salva = noBanco(senha);
        assertEquals(StatusSenha.PRESENTE, salva.getStatus());
        assertEquals(AGORA, salva.getCheckinEm());
    }

    @Test
    void cenario2PacienteCadastradoSemSenhaHoje() throws Exception {
        buscar(CPF_PACIENTE)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensagem").value("Paciente sem senha hoje"))
            .andExpect(jsonPath("$.pacienteId").value(paciente.getId()))
            .andExpect(jsonPath("$.senhas").isEmpty());
    }

    @Test
    void senhaDeOntemNaoVale() throws Exception {
        Senha senha = criarSenha(HOJE.minusDays(1), StatusSenha.AGUARDANDO);

        buscar(CPF_PACIENTE).andExpect(jsonPath("$.mensagem").value("Paciente sem senha hoje"));
        confirmar(recepcionista, senha.getId()).andExpect(status().isConflict());

        assertNull(noBanco(senha).getCheckinEm());
    }

    @Test
    void cpfSemCadastroTambemEstaSemSenha() throws Exception {
        buscar(CPF_SEM_CADASTRO)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensagem").value("Paciente sem senha hoje"))
            .andExpect(jsonPath("$.pacienteId").isEmpty());
    }

    @Test
    void numeroQueNaoEstaNaFila() throws Exception {
        buscar("99")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensagem").value("Nenhuma senha com esse número na fila de hoje"));
    }

    @Test
    void cpfInvalidoNaoEBuscado() throws Exception {
        buscar("12345678900")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem").value("CPF inválido"));
    }

    @Test
    void textoComLetrasNaoEBuscado() throws Exception {
        buscar("maria")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem").value("Informe o CPF (11 números) ou o número da senha"));
    }

    @Test
    void buscaVaziaPedeOCpfOuONumero() throws Exception {
        buscar("   ")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem").value("Informe o CPF ou o número da senha"));
    }

    @Test
    void presencaNaoEConfirmadaDuasVezes() throws Exception {
        Senha senha = criarSenha(HOJE, StatusSenha.PRESENTE);

        confirmar(recepcionista, senha.getId())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("A presença desta senha já foi confirmada"));
    }

    @Test
    void senhaCanceladaNaoFazCheckin() throws Exception {
        Senha senha = criarSenha(HOJE, StatusSenha.CANCELADA);

        confirmar(recepcionista, senha.getId())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Esta senha não está mais na fila"));

        assertEquals(StatusSenha.CANCELADA, noBanco(senha).getStatus());
    }

    @Test
    void senhaQueNaoExisteResponde404() throws Exception {
        confirmar(recepcionista, 999_999L).andExpect(status().isNotFound());
    }

    @Test
    void outroPerfilNaoFazCheckin() throws Exception {
        Senha senha = criarSenha(HOJE, StatusSenha.AGUARDANDO);

        confirmar(outroPerfil, senha.getId()).andExpect(status().isForbidden());

        assertEquals(StatusSenha.AGUARDANDO, noBanco(senha).getStatus());
    }
}
