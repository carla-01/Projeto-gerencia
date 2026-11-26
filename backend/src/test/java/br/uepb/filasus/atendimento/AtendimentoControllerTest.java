package br.uepb.filasus.atendimento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

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

/** Iniciar e finalizar o atendimento (US-14 - SCRUM-167). Dados fictícios. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AtendimentoControllerTest {

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
    private SenhaRepository senhaRepository;

    @Autowired
    private AtendimentoRepository atendimentoRepository;

    private final List<Paciente> pacientes = new ArrayList<>();
    private Usuario profissional;
    private Usuario colega;
    private Usuario recepcionista;
    private Servico clinico;
    private Servico odontologia;

    @BeforeEach
    void criarDados() {
        limparFila();
        clinico = servicoRepository.save(new Servico("Clínico"));
        odontologia = servicoRepository.save(new Servico("Odontologia"));

        profissional = new Usuario("Profissional Fictício", "profissional.atendimento@exemplo.test",
            protecaoCpf.hash("cpf-profissional-atendimento"), protecaoCpf.cifrar("11144477735"), "hash",
            Perfil.PROFISSIONAL);
        profissional.getSetores().add(clinico);
        profissional = usuarioRepository.save(profissional);

        colega = new Usuario("Colega Fictícia", "colega.atendimento@exemplo.test",
            protecaoCpf.hash("cpf-colega-atendimento"), protecaoCpf.cifrar("12345678909"), "hash",
            Perfil.PROFISSIONAL);
        colega.getSetores().add(clinico);
        colega = usuarioRepository.save(colega);

        recepcionista = usuarioRepository.save(new Usuario("Recepcionista Fictícia",
            "recepcao.atendimento@exemplo.test", protecaoCpf.hash("cpf-recepcao-atendimento"),
            protecaoCpf.cifrar("39053344705"), "hash", Perfil.RECEPCIONISTA));
    }

    /** Apaga só o que o teste criou. O atendimento sai antes da senha e os usuários antes dos serviços. */
    @AfterEach
    void limparDados() {
        atendimentoRepository.deleteAll();
        senhaRepository.deleteAll();
        usuarioRepository.delete(profissional);
        usuarioRepository.delete(colega);
        usuarioRepository.delete(recepcionista);
        for (Paciente paciente : pacientes) {
            pacienteRepository.delete(paciente);
        }
        pacientes.clear();
        servicoRepository.deleteAll();
    }

    private void limparFila() {
        atendimentoRepository.deleteAll();
        senhaRepository.deleteAll();
        servicoRepository.deleteAll();
    }

    private Senha criarSenha(Servico servico, int numero, StatusSenha situacao) {
        String chave = "paciente-atendimento-" + pacientes.size();
        Paciente paciente = pacienteRepository.save(new Paciente("Paciente " + numero, protecaoCpf.hash(chave),
            protecaoCpf.cifrar("52998224725"), Prioridade.NENHUMA, AGORA));
        pacientes.add(paciente);

        Senha senha = new Senha(servico, paciente, numero, HOJE, Prioridade.NENHUMA, AGORA);
        senha.setStatus(situacao);
        if (situacao != StatusSenha.AGUARDANDO && situacao != StatusSenha.PRESENTE) {
            senha.setChamadaEm(AGORA);
        }
        return senhaRepository.save(senha);
    }

    private Senha chamada(int numero) {
        return criarSenha(clinico, numero, StatusSenha.CHAMADA);
    }

    private ResultActions iniciar(Usuario quem, Senha senha) throws Exception {
        return mockMvc.perform(post("/api/profissional/atendimentos/" + senha.getId() + "/inicio")
            .header("Authorization", "Bearer " + jwtService.gerarToken(quem)));
    }

    private ResultActions finalizar(Usuario quem, Senha senha) throws Exception {
        return mockMvc.perform(post("/api/profissional/atendimentos/" + senha.getId() + "/fim")
            .header("Authorization", "Bearer " + jwtService.gerarToken(quem)));
    }

    private Senha noBanco(Senha senha) {
        return senhaRepository.findById(senha.getId()).get();
    }

    @Test
    void cenario1CicloCompletoGravaOsHorariosETiraDaFila() throws Exception {
        Senha senha = chamada(4);

        iniciar(profissional, senha)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.senhaId").value(senha.getId()))
            .andExpect(jsonPath("$.numero").value(4))
            .andExpect(jsonPath("$.nomePaciente").value("Paciente 4"))
            .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
            .andExpect(jsonPath("$.iniciadoEm").value("2026-11-25T12:00:00Z"))
            .andExpect(jsonPath("$.finalizadoEm").isEmpty());
        assertEquals(StatusSenha.EM_ATENDIMENTO, noBanco(senha).getStatus());

        finalizar(profissional, senha)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ATENDIDA"))
            .andExpect(jsonPath("$.iniciadoEm").value("2026-11-25T12:00:00Z"))
            .andExpect(jsonPath("$.finalizadoEm").value("2026-11-25T12:00:00Z"));

        assertEquals(StatusSenha.ATENDIDA, noBanco(senha).getStatus());
        Atendimento atendimento = atendimentoRepository.findBySenha_Id(senha.getId()).get();
        assertEquals(AGORA, atendimento.getIniciadoEm());
        assertEquals(AGORA, atendimento.getFinalizadoEm());
        // Saiu da fila: não está mais entre as senhas ativas do paciente.
        assertTrue(senhaRepository.buscarSenhasAtivas(noBanco(senha).getPaciente(), HOJE).isEmpty());
    }

    @Test
    void cenario2FinalizarSemIniciarPedeParaIniciarAntes() throws Exception {
        Senha senha = chamada(1);

        finalizar(profissional, senha)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Inicie o atendimento antes de finalizar"));

        assertEquals(StatusSenha.CHAMADA, noBanco(senha).getStatus());
        assertTrue(atendimentoRepository.findBySenha_Id(senha.getId()).isEmpty());
    }

    @Test
    void naoIniciaDuasVezes() throws Exception {
        Senha senha = chamada(1);
        iniciar(profissional, senha).andExpect(status().isOk());

        iniciar(profissional, senha)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("O atendimento desta senha já foi iniciado"));

        assertEquals(1, atendimentoRepository.count());
    }

    @Test
    void naoFinalizaDuasVezes() throws Exception {
        Senha senha = chamada(1);
        iniciar(profissional, senha).andExpect(status().isOk());
        finalizar(profissional, senha).andExpect(status().isOk());

        finalizar(profissional, senha)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("O atendimento desta senha já foi finalizado"));
    }

    @Test
    void naoIniciaSenhaQueNaoFoiChamada() throws Exception {
        Senha presente = criarSenha(clinico, 1, StatusSenha.PRESENTE);

        iniciar(profissional, presente)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Esta senha não foi chamada"));

        assertEquals(StatusSenha.PRESENTE, noBanco(presente).getStatus());
    }

    @Test
    void soQuemIniciouFinaliza() throws Exception {
        Senha senha = chamada(1);
        iniciar(profissional, senha).andExpect(status().isOk());

        finalizar(colega, senha)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Este atendimento foi iniciado por outro profissional"));

        assertEquals(StatusSenha.EM_ATENDIMENTO, noBanco(senha).getStatus());
        assertNull(atendimentoRepository.findBySenha_Id(senha.getId()).get().getFinalizadoEm());
    }

    @Test
    void senhaDeOutroSetorNaoEEncontrada() throws Exception {
        Senha outroSetor = criarSenha(odontologia, 1, StatusSenha.CHAMADA);

        iniciar(profissional, outroSetor)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensagem").value("Senha não encontrada"));

        assertEquals(StatusSenha.CHAMADA, noBanco(outroSetor).getStatus());
    }

    @Test
    void outroPerfilNaoInicia() throws Exception {
        Senha senha = chamada(1);

        iniciar(recepcionista, senha).andExpect(status().isForbidden());

        assertEquals(StatusSenha.CHAMADA, noBanco(senha).getStatus());
    }
}
