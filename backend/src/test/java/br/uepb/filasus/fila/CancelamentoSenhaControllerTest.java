package br.uepb.filasus.fila;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** Cancelamento da senha pelo paciente (US-08 - SCRUM-43). Dados fictícios. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CancelamentoSenhaControllerTest {

    /** 12:00 UTC = 09:00 no horário da UBS. */
    private static final Instant AGORA = Instant.parse("2026-11-10T12:00:00Z");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);
    private static final String CPF = "52998224725";
    private static final String CPF_OUTRO = "11144477735";

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
    private Usuario outroUsuario;
    private Paciente outroPaciente;
    private Servico clinico;

    @BeforeEach
    void criarPacientesEServico() {
        limparFila();
        usuario = usuarioRepository.save(new Usuario("Paciente Fictício", "paciente.cancela@exemplo.test",
            protecaoCpf.hash(CPF), protecaoCpf.cifrar(CPF), "hash", Perfil.PACIENTE));
        paciente = salvarPaciente("Paciente Fictício", CPF, usuario);

        outroUsuario = usuarioRepository.save(new Usuario("Outro Paciente", "outro.cancela@exemplo.test",
            protecaoCpf.hash(CPF_OUTRO), protecaoCpf.cifrar(CPF_OUTRO), "hash", Perfil.PACIENTE));
        outroPaciente = salvarPaciente("Outro Paciente", CPF_OUTRO, outroUsuario);

        clinico = servicoRepository.save(new Servico("Clínico"));
        configuracaoServicoRepository.save(new ConfiguracaoServico(clinico,
            LocalTime.of(7, 0), LocalTime.of(17, 0), 40));
    }

    /** Apaga só o que o teste criou, para não atrapalhar a limpeza dos outros testes. */
    @AfterEach
    void limparDados() {
        limparFila();
        pacienteRepository.delete(paciente);
        pacienteRepository.delete(outroPaciente);
        usuarioRepository.delete(usuario);
        usuarioRepository.delete(outroUsuario);
    }

    private void limparFila() {
        senhaRepository.deleteAll();
        configuracaoServicoRepository.deleteAll();
        servicoRepository.deleteAll();
    }

    private Paciente salvarPaciente(String nome, String cpf, Usuario dono) {
        Paciente novo = new Paciente(nome, protecaoCpf.hash(cpf), protecaoCpf.cifrar(cpf), Prioridade.NENHUMA, AGORA);
        novo.setUsuario(dono);
        return pacienteRepository.save(novo);
    }

    private Senha senhaDeHoje(Paciente dono, int numero, StatusSenha status) {
        Senha senha = new Senha(clinico, dono, numero, HOJE, Prioridade.NENHUMA, AGORA);
        senha.setStatus(status);
        return senhaRepository.save(senha);
    }

    private ResultActions cancelar(Usuario quemCancela, Long senhaId) throws Exception {
        return mockMvc.perform(post("/api/paciente/senhas/" + senhaId + "/cancelar")
            .header("Authorization", "Bearer " + jwtService.gerarToken(quemCancela)));
    }

    private StatusSenha statusNoBanco(Senha senha) {
        return senhaRepository.findById(senha.getId()).get().getStatus();
    }

    /** Senhas que ainda contam na fila (AGUARDANDO ou PRESENTE). */
    private int senhasNaFila() {
        int quantidade = 0;
        for (Senha senha : senhaRepository.findAll()) {
            if (senha.getStatus() == StatusSenha.AGUARDANDO || senha.getStatus() == StatusSenha.PRESENTE) {
                quantidade = quantidade + 1;
            }
        }
        return quantidade;
    }

    @Test
    void pacienteCancelaASenhaQueEstaAguardando() throws Exception {
        Senha minha = senhaDeHoje(paciente, 1, StatusSenha.AGUARDANDO);

        cancelar(usuario, minha.getId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numero").value(1))
            .andExpect(jsonPath("$.status").value("CANCELADA"));

        assertEquals(StatusSenha.CANCELADA, statusNoBanco(minha));
    }

    @Test
    void pacienteCancelaASenhaDepoisDoCheckin() throws Exception {
        Senha minha = senhaDeHoje(paciente, 1, StatusSenha.PRESENTE);

        cancelar(usuario, minha.getId()).andExpect(status().isOk());

        assertEquals(StatusSenha.CANCELADA, statusNoBanco(minha));
    }

    @Test
    void senhaCanceladaSaiDaFilaEQuemEstavaAtrasAnda() throws Exception {
        Senha minha = senhaDeHoje(paciente, 1, StatusSenha.AGUARDANDO);
        senhaDeHoje(outroPaciente, 2, StatusSenha.AGUARDANDO);
        assertEquals(2, senhasNaFila());

        cancelar(usuario, minha.getId()).andExpect(status().isOk());

        assertEquals(1, senhasNaFila());
    }

    @Test
    void naoCancelaQuandoOPacienteJaFoiChamado() throws Exception {
        Senha minha = senhaDeHoje(paciente, 1, StatusSenha.CHAMADA);

        cancelar(usuario, minha.getId())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Seu atendimento já começou"));

        assertEquals(StatusSenha.CHAMADA, statusNoBanco(minha));
    }

    @Test
    void naoCancelaDuranteOAtendimento() throws Exception {
        Senha minha = senhaDeHoje(paciente, 1, StatusSenha.EM_ATENDIMENTO);

        cancelar(usuario, minha.getId())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Seu atendimento já começou"));

        assertEquals(StatusSenha.EM_ATENDIMENTO, statusNoBanco(minha));
    }

    @Test
    void naoCancelaDuasVezes() throws Exception {
        Senha minha = senhaDeHoje(paciente, 1, StatusSenha.CANCELADA);

        cancelar(usuario, minha.getId())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Esta senha não está mais na fila"));
    }

    @Test
    void naoCancelaASenhaDeOutroPaciente() throws Exception {
        Senha doOutro = senhaDeHoje(outroPaciente, 1, StatusSenha.AGUARDANDO);

        cancelar(usuario, doOutro.getId())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensagem").value("Senha não encontrada"));

        assertEquals(StatusSenha.AGUARDANDO, statusNoBanco(doOutro));
    }

    @Test
    void senhaInexistenteRecebe404() throws Exception {
        cancelar(usuario, 999999L)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensagem").value("Senha não encontrada"));
    }

    @Test
    void outroPerfilRecebe403() throws Exception {
        Senha minha = senhaDeHoje(paciente, 1, StatusSenha.AGUARDANDO);
        Usuario recepcionista = new Usuario("Recepção Fictícia", "recepcao.cancela@exemplo.test",
            "cpf-hash", "cpf-cifrado", "hash", Perfil.RECEPCIONISTA);

        cancelar(recepcionista, minha.getId()).andExpect(status().isForbidden());

        assertEquals(StatusSenha.AGUARDANDO, statusNoBanco(minha));
    }

    @Test
    void semTokenRecebe401() throws Exception {
        Senha minha = senhaDeHoje(paciente, 1, StatusSenha.AGUARDANDO);

        mockMvc.perform(post("/api/paciente/senhas/" + minha.getId() + "/cancelar"))
            .andExpect(status().isUnauthorized());
    }
}
