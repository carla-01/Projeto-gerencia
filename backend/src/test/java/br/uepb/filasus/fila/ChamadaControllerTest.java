package br.uepb.filasus.fila;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import br.uepb.filasus.lgpd.ProtecaoCpf;
import br.uepb.filasus.paciente.Paciente;
import br.uepb.filasus.paciente.PacienteRepository;
import br.uepb.filasus.paciente.Prioridade;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;

/** Chamada do próximo paciente (US-13 - SCRUM-162). Dados fictícios. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChamadaControllerTest {

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

    private final List<Paciente> pacientes = new ArrayList<>();
    private Usuario profissional;
    private Usuario semSetor;
    private Usuario recepcionista;
    private Servico clinico;
    private Servico odontologia;

    @BeforeEach
    void criarDados() {
        limparFila();
        clinico = servicoRepository.save(new Servico("Clínico"));
        odontologia = servicoRepository.save(new Servico("Odontologia"));

        profissional = new Usuario("Profissional Fictício", "profissional.chamada@exemplo.test",
            protecaoCpf.hash("cpf-profissional-chamada"), protecaoCpf.cifrar("11144477735"), "hash",
            Perfil.PROFISSIONAL);
        profissional.getSetores().add(clinico);
        profissional = usuarioRepository.save(profissional);

        semSetor = usuarioRepository.save(new Usuario("Profissional Sem Setor", "semsetor.chamada@exemplo.test",
            protecaoCpf.hash("cpf-sem-setor-chamada"), protecaoCpf.cifrar("12345678909"), "hash",
            Perfil.PROFISSIONAL));
        recepcionista = usuarioRepository.save(new Usuario("Recepcionista Fictícia", "recepcao.chamada@exemplo.test",
            protecaoCpf.hash("cpf-recepcao-chamada"), protecaoCpf.cifrar("39053344705"), "hash",
            Perfil.RECEPCIONISTA));
    }

    /** Apaga só o que o teste criou. Os usuários saem antes dos serviços por causa do setor. */
    @AfterEach
    void limparDados() {
        senhaRepository.deleteAll();
        usuarioRepository.delete(profissional);
        usuarioRepository.delete(semSetor);
        usuarioRepository.delete(recepcionista);
        for (Paciente paciente : pacientes) {
            pacienteRepository.delete(paciente);
        }
        pacientes.clear();
        servicoRepository.deleteAll();
    }

    private void limparFila() {
        senhaRepository.deleteAll();
        servicoRepository.deleteAll();
    }

    private Senha criarSenha(Servico servico, int numero, StatusSenha situacao, Prioridade prioridade, LocalDate dia) {
        String chave = "paciente-chamada-" + pacientes.size();
        Paciente paciente = pacienteRepository.save(new Paciente("Paciente " + numero, protecaoCpf.hash(chave),
            protecaoCpf.cifrar("52998224725"), Prioridade.NENHUMA, AGORA));
        pacientes.add(paciente);

        Senha senha = new Senha(servico, paciente, numero, dia, prioridade, AGORA);
        senha.setStatus(situacao);
        return senhaRepository.save(senha);
    }

    private Senha presente(int numero) {
        return criarSenha(clinico, numero, StatusSenha.PRESENTE, Prioridade.NENHUMA, HOJE);
    }

    private ResultActions chamar(Usuario quem) throws Exception {
        return mockMvc.perform(post("/api/profissional/chamadas")
            .header("Authorization", "Bearer " + jwtService.gerarToken(quem)));
    }

    private Senha noBanco(Senha senha) {
        return senhaRepository.findById(senha.getId()).get();
    }

    @Test
    void cenario1ChamaOPacientePresenteEGuardaOHorario() throws Exception {
        Senha senha = presente(4);

        chamar(profissional)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.senha.numero").value(4))
            .andExpect(jsonPath("$.senha.servico").value("Clínico"))
            .andExpect(jsonPath("$.senha.nomePaciente").value("Paciente 4"))
            .andExpect(jsonPath("$.senha.status").value("CHAMADA"))
            .andExpect(jsonPath("$.senha.chamadaEm").value("2026-11-25T12:00:00Z"));

        Senha salva = noBanco(senha);
        assertEquals(StatusSenha.CHAMADA, salva.getStatus());
        assertEquals(AGORA, salva.getChamadaEm());
    }

    @Test
    void respeitaAOrdemDePrioridade() throws Exception {
        presente(1);
        criarSenha(clinico, 2, StatusSenha.PRESENTE, Prioridade.IDOSO, HOJE);
        criarSenha(clinico, 3, StatusSenha.PRESENTE, Prioridade.URGENCIA, HOJE);

        chamar(profissional).andExpect(jsonPath("$.senha.numero").value(3));
        chamar(profissional).andExpect(jsonPath("$.senha.numero").value(2));
        chamar(profissional).andExpect(jsonPath("$.senha.numero").value(1));
        chamar(profissional).andExpect(jsonPath("$.mensagem").value("Não há pacientes aguardando"));
    }

    @Test
    void quemNaoFezCheckinNaoEChamado() throws Exception {
        Senha aguardando = criarSenha(clinico, 1, StatusSenha.AGUARDANDO, Prioridade.URGENCIA, HOJE);
        presente(2);

        chamar(profissional).andExpect(jsonPath("$.senha.numero").value(2));

        assertEquals(StatusSenha.AGUARDANDO, noBanco(aguardando).getStatus());
    }

    @Test
    void soChamaAFilaDoProprioSetor() throws Exception {
        Senha outroSetor = criarSenha(odontologia, 1, StatusSenha.PRESENTE, Prioridade.URGENCIA, HOJE);

        chamar(profissional).andExpect(jsonPath("$.mensagem").value("Não há pacientes aguardando"));

        assertEquals(StatusSenha.PRESENTE, noBanco(outroSetor).getStatus());
    }

    @Test
    void senhaDeOntemNaoEChamada() throws Exception {
        Senha ontem = criarSenha(clinico, 1, StatusSenha.PRESENTE, Prioridade.NENHUMA, HOJE.minusDays(1));

        chamar(profissional).andExpect(jsonPath("$.mensagem").value("Não há pacientes aguardando"));

        assertNull(noBanco(ontem).getChamadaEm());
    }

    @Test
    void cenario2FilaVazia() throws Exception {
        chamar(profissional)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensagem").value("Não há pacientes aguardando"))
            .andExpect(jsonPath("$.senha").isEmpty());
    }

    @Test
    void profissionalSemSetorRecebeAviso() throws Exception {
        chamar(semSetor)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Seu usuário não tem setor. Peça ao gestor para configurar"));
    }

    @Test
    void outroPerfilNaoChama() throws Exception {
        Senha senha = presente(1);

        chamar(recepcionista).andExpect(status().isForbidden());

        assertEquals(StatusSenha.PRESENTE, noBanco(senha).getStatus());
    }
}
