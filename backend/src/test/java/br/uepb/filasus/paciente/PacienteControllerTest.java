package br.uepb.filasus.paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.uepb.filasus.auth.LoginRequest;
import br.uepb.filasus.lgpd.ProtecaoCpf;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;

/**
 * Testes do POST /pacientes/autocadastro (US-04 - SCRUM-39).
 * Só com dados fictícios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PacienteControllerTest {

    /** CPF fictício com dígitos verificadores válidos. */
    private static final String CPF = "52998224725";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ConsentimentoRepository consentimentoRepository;

    @Autowired
    private ProtecaoCpf protecaoCpf;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Os outros testes apagam usuários: pacientes e aceites ligados a eles saem antes. */
    @BeforeEach
    @AfterEach
    void limparBanco() {
        consentimentoRepository.deleteAll();
        pacienteRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private AutocadastroRequest pedido(String cpf, String email, boolean aceiteTermo) {
        return new AutocadastroRequest("Maria da Silva", cpf, "898001234567890", "83999990000",
            email, "senhaForte123", Prioridade.GESTANTE, aceiteTermo);
    }

    private ResultActions autocadastrar(AutocadastroRequest pedido) throws Exception {
        return mockMvc.perform(post("/pacientes/autocadastro")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(pedido)));
    }

    /** CT-US04-01: a conta é criada e o aceite fica registrado com data e hora. */
    @Test
    void cadastroComSucessoRegistraOAceite() throws Exception {
        autocadastrar(pedido("529.982.247-25", "maria@email.com", true))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.pacienteId").isNumber())
            .andExpect(jsonPath("$.nome").value("Maria da Silva"))
            .andExpect(jsonPath("$.prioridade").value("GESTANTE"))
            .andExpect(jsonPath("$.versaoTermo").value("1.0"))
            .andExpect(jsonPath("$.aceitoEm").isNotEmpty())
            .andExpect(jsonPath("$.cpf").doesNotExist());

        // A conta de login foi criada como PACIENTE, com senha em bcrypt.
        Usuario usuario = usuarioRepository.findByCpfHash(protecaoCpf.hash(CPF)).orElseThrow();
        assertEquals(Perfil.PACIENTE, usuario.getPerfil());
        assertTrue(passwordEncoder.matches("senhaForte123", usuario.getSenhaHash()));

        // O paciente está ligado à conta e o CPF não está em texto claro.
        Paciente paciente = pacienteRepository.findByCpfHash(protecaoCpf.hash(CPF)).orElseThrow();
        assertEquals(usuario.getId(), paciente.getUsuario().getId());
        assertEquals("898001234567890", paciente.getCartaoSus());
        assertEquals(CPF, protecaoCpf.decifrar(paciente.getCpfCifrado()));

        // O aceite ficou registrado com a versão do termo e a data e hora.
        List<Consentimento> aceites = consentimentoRepository.findByPacienteId(paciente.getId());
        assertEquals(1, aceites.size());
        assertTrue(aceites.get(0).isAceito());
        assertEquals("1.0", aceites.get(0).getVersaoTermo());
    }

    @Test
    void depoisDoCadastroOPacienteEntraComCpfESenha() throws Exception {
        autocadastrar(pedido(CPF, "maria@email.com", true)).andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("529.982.247-25", "senhaForte123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.perfil").value("PACIENTE"));
    }

    /** CT-US04-02: sem o aceite, o cadastro não é concluído e mostra a finalidade dos dados. */
    @Test
    void semAceiteNaoCadastraEMostraAFinalidade() throws Exception {
        autocadastrar(pedido(CPF, "maria@email.com", false))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("aceiteTermo"))
            .andExpect(jsonPath("$.mensagem").value(PacienteService.FINALIDADE_DOS_DADOS));

        assertEquals(0, usuarioRepository.count());
        assertEquals(0, pacienteRepository.count());
        assertEquals(0, consentimentoRepository.count());
    }

    /** CT-US04-03: CPF com dígito verificador errado. */
    @Test
    void cpfInvalidoNaoCadastra() throws Exception {
        autocadastrar(pedido("52998224700", "maria@email.com", true))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("cpf"))
            .andExpect(jsonPath("$.mensagem").value("CPF inválido"));

        assertEquals(0, pacienteRepository.count());
    }

    @Test
    void campoObrigatorioFaltandoDiz400ComOCampo() throws Exception {
        AutocadastroRequest semCartao = new AutocadastroRequest("Maria da Silva", CPF, "", null,
            "maria@email.com", "senhaForte123", null, true);

        autocadastrar(semCartao)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("cartaoSus"));
    }

    @Test
    void cpfJaCadastradoDiz409() throws Exception {
        autocadastrar(pedido(CPF, "maria@email.com", true)).andExpect(status().isCreated());

        autocadastrar(pedido(CPF, "outro@email.com", true))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Já existe um cadastro com este CPF"));

        assertEquals(1, pacienteRepository.count());
    }

    @Test
    void emailJaCadastradoDiz409() throws Exception {
        autocadastrar(pedido(CPF, "maria@email.com", true)).andExpect(status().isCreated());

        autocadastrar(pedido("11144477735", "maria@email.com", true))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Já existe um cadastro com este e-mail"));

        assertEquals(1, pacienteRepository.count());
    }
}
