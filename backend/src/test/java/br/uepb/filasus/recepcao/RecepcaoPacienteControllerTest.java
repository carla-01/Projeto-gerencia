package br.uepb.filasus.recepcao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.uepb.filasus.auth.JwtService;
import br.uepb.filasus.lgpd.ProtecaoCpf;
import br.uepb.filasus.paciente.CadastroPresencialRequest;
import br.uepb.filasus.paciente.Consentimento;
import br.uepb.filasus.paciente.ConsentimentoRepository;
import br.uepb.filasus.paciente.Paciente;
import br.uepb.filasus.paciente.PacienteRepository;
import br.uepb.filasus.paciente.Prioridade;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;

/**
 * Cadastro de paciente pela recepção (US05 - SCRUM-40), pelo POST /api/recepcao/pacientes.
 * Só com dados fictícios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecepcaoPacienteControllerTest {

    /** CPF fictício do paciente, com dígitos verificadores válidos. */
    private static final String CPF_PACIENTE = "52998224725";

    /** CPF fictício da recepcionista, com dígitos verificadores válidos. */
    private static final String CPF_RECEPCIONISTA = "11144477735";

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
    private JwtService jwtService;

    private Usuario recepcionista;

    /** Os outros testes apagam usuários: pacientes e aceites ligados a eles saem antes. */
    @BeforeEach
    void prepararCenario() {
        limparBanco();
        recepcionista = usuarioRepository.save(new Usuario("Ana Souza", "recepcao@ubs.com",
            protecaoCpf.hash(CPF_RECEPCIONISTA), protecaoCpf.cifrar(CPF_RECEPCIONISTA), "hash", Perfil.RECEPCIONISTA));
    }

    @AfterEach
    void limparBanco() {
        consentimentoRepository.deleteAll();
        pacienteRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private CadastroPresencialRequest pedido(String cpf, boolean consentimentoVerbal) {
        return new CadastroPresencialRequest("Antônio Lima", cpf, "700123456789012", "8333330000",
            Prioridade.IDOSO, consentimentoVerbal);
    }

    private ResultActions cadastrar(CadastroPresencialRequest pedido, Usuario logado) throws Exception {
        return mockMvc.perform(post("/api/recepcao/pacientes")
            .header("Authorization", "Bearer " + jwtService.gerarToken(logado))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(pedido)));
    }

    // CT-US05-01 - Cadastro presencial
    // Dado que estou logado como recepcionista
    // Quando cadastro o paciente e registro o consentimento verbal
    // Então o paciente fica disponível para entrar na fila
    @Test
    @DisplayName("CT-US05-01 - Cadastro presencial: recepcionista cadastra o paciente com consentimento verbal")
    void cadastroPresencialComConsentimentoVerbal() throws Exception {
        MvcResult resultado = cadastrar(pedido("529.982.247-25", true), recepcionista)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.pacienteId").isNumber())
            .andExpect(jsonPath("$.nome").value("Antônio Lima"))
            .andExpect(jsonPath("$.prioridade").value("IDOSO"))
            .andExpect(jsonPath("$.jaCadastrado").value(false))
            .andExpect(jsonPath("$.cpfMascarado").value("529.***.***-25"))
            .andExpect(jsonPath("$.versaoTermo").value("1.0"))
            .andExpect(jsonPath("$.consentimentoEm").isNotEmpty())
            // O CPF não volta na resposta.
            .andExpect(jsonPath("$.cpf").doesNotExist())
            .andReturn();

        JsonNode resposta = objectMapper.readTree(resultado.getResponse().getContentAsString());
        Long pacienteId = resposta.get("pacienteId").asLong();

        // O paciente fica disponível: dá para encontrá-lo pelo CPF para colocar na fila.
        Optional<Paciente> encontrado = pacienteRepository.findByCpfHash(protecaoCpf.hash(CPF_PACIENTE));
        assertTrue(encontrado.isPresent());
        Paciente paciente = encontrado.get();
        assertEquals(pacienteId, paciente.getId());
        assertEquals("700123456789012", paciente.getCartaoSus());

        // Sem login próprio, e com o registro de qual recepcionista cadastrou.
        assertNull(paciente.getUsuario());
        assertEquals(recepcionista.getId(), paciente.getCadastradoPor().getId());

        // O CPF não fica em texto puro no banco.
        assertNotEquals(CPF_PACIENTE, paciente.getCpfHash());
        assertFalse(paciente.getCpfCifrado().contains(CPF_PACIENTE));

        // O consentimento verbal ficou registrado com a versão do termo e o horário.
        List<Consentimento> consentimentos = consentimentoRepository.findByPacienteId(pacienteId);
        assertEquals(1, consentimentos.size());
        assertTrue(consentimentos.get(0).isAceito());
        assertEquals("1.0", consentimentos.get(0).getVersaoTermo());
    }

    @Test
    @DisplayName("CT-US05-01 - Cadastro presencial: sem o consentimento verbal o paciente não é cadastrado")
    void cadastroSemConsentimentoVerbalERecusado() throws Exception {
        cadastrar(pedido(CPF_PACIENTE, false), recepcionista)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("consentimentoVerbal"))
            .andExpect(jsonPath("$.mensagem").value("Registre o consentimento verbal do paciente antes de cadastrar."));

        assertEquals(0, pacienteRepository.count());
        assertEquals(0, consentimentoRepository.count());
    }

    @Test
    @DisplayName("CT-US05-01 - Cadastro presencial: CPF inválido não é cadastrado")
    void cadastroComCpfInvalidoERecusado() throws Exception {
        cadastrar(pedido("123.456.789-00", true), recepcionista)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("cpf"))
            .andExpect(jsonPath("$.mensagem").value("CPF inválido"));

        assertEquals(0, pacienteRepository.count());
    }

    @Test
    @DisplayName("CT-US05-01 - Cadastro presencial: sem cartão SUS não é cadastrado")
    void cadastroSemCartaoSusERecusado() throws Exception {
        CadastroPresencialRequest semCartao = new CadastroPresencialRequest("Antônio Lima", CPF_PACIENTE, "",
            null, null, true);

        cadastrar(semCartao, recepcionista)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("cartaoSus"));
    }

    // CT-US05-02 - Paciente já cadastrado
    // Dado que o CPF já existe na base
    // Quando tento cadastrar de novo
    // Então o sistema abre o cadastro existente em vez de duplicar
    @Test
    @DisplayName("CT-US05-02 - Paciente já cadastrado: devolve o cadastro existente em vez de duplicar")
    void cpfJaCadastradoDevolveOCadastroExistente() throws Exception {
        MvcResult primeiro = cadastrar(pedido(CPF_PACIENTE, true), recepcionista)
            .andExpect(status().isCreated())
            .andReturn();
        Long pacienteId = objectMapper.readTree(primeiro.getResponse().getContentAsString()).get("pacienteId").asLong();

        // Mesmo CPF (agora com máscara), com outro nome e outra prioridade.
        CadastroPresencialRequest deNovo = new CadastroPresencialRequest("Antonio de Lima", "529.982.247-25",
            "700999999999999", null, Prioridade.NENHUMA, true);

        cadastrar(deNovo, recepcionista)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jaCadastrado").value(true))
            .andExpect(jsonPath("$.pacienteId").value(pacienteId))
            // Os dados são os do cadastro existente: nada foi alterado.
            .andExpect(jsonPath("$.nome").value("Antônio Lima"))
            .andExpect(jsonPath("$.prioridade").value("IDOSO"))
            .andExpect(jsonPath("$.cpfMascarado").value("529.***.***-25"))
            .andExpect(jsonPath("$.cadastradoEm").isNotEmpty())
            .andExpect(jsonPath("$.versaoTermo").value("1.0"));

        // Não duplicou o paciente nem registrou outro consentimento.
        assertEquals(1, pacienteRepository.count());
        assertEquals(1, consentimentoRepository.count());
        assertEquals("700123456789012", pacienteRepository.findById(pacienteId).get().getCartaoSus());
    }

    @Test
    @DisplayName("CT-US05-02 - Paciente já cadastrado: também abre quem se cadastrou pelo celular")
    void cpfDoAutocadastroDevolveOCadastroExistente() throws Exception {
        // Paciente que fez o autocadastro (US-04): tem login próprio e aceitou o termo pelo celular.
        Usuario login = usuarioRepository.save(new Usuario("Maria da Silva", "maria@email.com",
            protecaoCpf.hash(CPF_PACIENTE), protecaoCpf.cifrar(CPF_PACIENTE), "hash", Perfil.PACIENTE));
        Paciente maria = new Paciente("Maria da Silva", protecaoCpf.hash(CPF_PACIENTE),
            protecaoCpf.cifrar(CPF_PACIENTE), Prioridade.GESTANTE, Instant.parse("2026-10-21T10:00:00Z"));
        maria.setUsuario(login);
        maria = pacienteRepository.save(maria);
        consentimentoRepository.save(new Consentimento(maria, "1.0", true, Instant.parse("2026-10-21T10:00:00Z")));

        cadastrar(pedido(CPF_PACIENTE, true), recepcionista)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jaCadastrado").value(true))
            .andExpect(jsonPath("$.pacienteId").value(maria.getId()))
            .andExpect(jsonPath("$.nome").value("Maria da Silva"))
            .andExpect(jsonPath("$.prioridade").value("GESTANTE"));

        assertEquals(1, pacienteRepository.count());
        assertEquals(1, consentimentoRepository.count());
    }

    // "Dado que estou logado como recepcionista": os outros perfis não podem cadastrar.
    @Test
    @DisplayName("CT-US05-01 - Cadastro presencial: só a recepcionista cadastra (outros perfis recebem 403)")
    void outroPerfilRecebe403() throws Exception {
        Usuario paciente = usuarioRepository.save(new Usuario("Maria da Silva", "maria@email.com",
            protecaoCpf.hash(CPF_PACIENTE), protecaoCpf.cifrar(CPF_PACIENTE), "hash", Perfil.PACIENTE));

        cadastrar(pedido(CPF_PACIENTE, true), paciente)
            .andExpect(status().isForbidden());

        assertEquals(0, pacienteRepository.count());
    }

    @Test
    @DisplayName("CT-US05-01 - Cadastro presencial: sem login recebe 401")
    void semLoginRecebe401() throws Exception {
        mockMvc.perform(post("/api/recepcao/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pedido(CPF_PACIENTE, true))))
            .andExpect(status().isUnauthorized());

        assertEquals(0, pacienteRepository.count());
    }
}
