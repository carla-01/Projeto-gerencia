package br.uepb.filasus.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import br.uepb.filasus.auth.JwtService;
import br.uepb.filasus.lgpd.ProtecaoCpf;

/** Back-end da US03 - SCRUM-15: o gestor cadastra recepcionistas e profissionais. Dados fictícios. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FuncionarioControllerTest {

    private static final String ROTA = "/api/gestor/funcionarios";
    private static final String SENHA_INICIAL = "inicial123";
    /** CPFs fictícios com dígitos verificadores válidos. */
    private static final String CPF = "529.982.247-25";
    private static final String OUTRO_CPF = "111.444.777-35";
    private static final String CARTAO_SUS = "898001234567891";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProtecaoCpf protecaoCpf;

    @BeforeEach
    @AfterEach
    void limparUsuarios() {
        usuarioRepository.deleteAll();
    }

    // Cenário 1: Cadastro de funcionário

    @Test
    void gestorCadastraProfissionalComSenhaEmHash() throws Exception {
        cadastrar(Perfil.GESTOR, corpo("Profissional Fictícia", "profissional@exemplo.test", "PROFISSIONAL", SENHA_INICIAL))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.nome").value("Profissional Fictícia"))
            .andExpect(jsonPath("$.email").value("profissional@exemplo.test"))
            .andExpect(jsonPath("$.perfil").value("PROFISSIONAL"))
            .andExpect(jsonPath("$.ativo").value(true))
            .andExpect(jsonPath("$.senhaInicial").doesNotExist())
            .andExpect(jsonPath("$.senhaHash").doesNotExist());

        Usuario salvo = usuarioRepository.findByEmail("profissional@exemplo.test").orElseThrow();
        assertEquals(Perfil.PROFISSIONAL, salvo.getPerfil());
        assertTrue(salvo.isAtivo());
        assertNotEquals(SENHA_INICIAL, salvo.getSenhaHash());
        assertTrue(passwordEncoder.matches(SENHA_INICIAL, salvo.getSenhaHash()));
        assertEquals(protecaoCpf.hash("52998224725"), salvo.getCpfHash());
        assertEquals("52998224725", protecaoCpf.decifrar(salvo.getCpfCifrado()));
        assertEquals(CARTAO_SUS, salvo.getCartaoSus());
    }

    @Test
    void gestorCadastraRecepcionista() throws Exception {
        cadastrar(Perfil.GESTOR, corpo("Recepcionista Fictícia", "recepcao@exemplo.test", "RECEPCIONISTA", SENHA_INICIAL))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.perfil").value("RECEPCIONISTA"));

        assertEquals(1, usuarioRepository.count());
    }

    @Test
    void funcionarioCadastradoRecebeAcessoComASenhaInicial() throws Exception {
        cadastrar(Perfil.GESTOR, corpo("Profissional Fictícia", "acesso@exemplo.test", "PROFISSIONAL", SENHA_INICIAL))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"acesso@exemplo.test\",\"senha\":\"" + SENHA_INICIAL + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.perfil").value("PROFISSIONAL"));
    }

    // Cenário 2: E-mail duplicado

    @Test
    void emailDuplicadoRecebe409ENadaEGravado() throws Exception {
        salvarExistente("repetido@exemplo.test", OUTRO_CPF);

        cadastrar(Perfil.GESTOR, corpo("Outra Pessoa", "repetido@exemplo.test", "PROFISSIONAL", SENHA_INICIAL))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));

        assertEquals(1, usuarioRepository.count());
        assertEquals(Perfil.RECEPCIONISTA, usuarioRepository.findByEmail("repetido@exemplo.test").orElseThrow().getPerfil());
    }

    // CPF do funcionário (exigido pela tabela usuario do DER)

    @Test
    void cpfDuplicadoRecebe409ENadaEGravado() throws Exception {
        salvarExistente("existente@exemplo.test", CPF);

        cadastrar(Perfil.GESTOR, corpo("Outra Pessoa", "novo@exemplo.test", "PROFISSIONAL", SENHA_INICIAL))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("CPF já cadastrado"));

        assertEquals(1, usuarioRepository.count());
    }

    @Test
    void cpfInvalidoRecebe400() throws Exception {
        cadastrar(Perfil.GESTOR, corpo("Pessoa Fictícia", "cpf@exemplo.test", "123.456.789-00", "PROFISSIONAL", SENHA_INICIAL))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("cpf"))
            .andExpect(jsonPath("$.mensagem").value("CPF inválido"));

        assertEquals(0, usuarioRepository.count());
    }

    @Test
    void cartaoSusSemOs15NumerosRecebe400() throws Exception {
        for (String cartao : new String[] {" ", "89800123456789", "8980012345678901", "89800123456789a"}) {
            cadastrar(Perfil.GESTOR, corpo("Pessoa Fictícia", "sus@exemplo.test", CPF, cartao, "PROFISSIONAL", SENHA_INICIAL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("cartaoSus"));
        }

        cadastrar(Perfil.GESTOR, corpo("Pessoa Fictícia", "sus@exemplo.test", CPF, "123", "PROFISSIONAL", SENHA_INICIAL))
            .andExpect(jsonPath("$.mensagem").value("O cartão SUS tem 15 números"));

        assertEquals(0, usuarioRepository.count());
    }

    @Test
    void cpfEmBrancoRecebe400() throws Exception {
        cadastrar(Perfil.GESTOR, corpo("Pessoa Fictícia", "cpf@exemplo.test", " ", "PROFISSIONAL", SENHA_INICIAL))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("cpf"))
            .andExpect(jsonPath("$.mensagem").value("CPF é obrigatório"));

        assertEquals(0, usuarioRepository.count());
    }

    // Regras da PO (refinamento de 16/11)

    @Test
    void senhaInicialComMenosDe8CaracteresRecebe400() throws Exception {
        cadastrar(Perfil.GESTOR, corpo("Profissional Fictícia", "curta@exemplo.test", "PROFISSIONAL", "1234567"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("senhaInicial"))
            .andExpect(jsonPath("$.mensagem").value("Senha inicial deve ter entre 8 e 72 caracteres"));

        assertEquals(0, usuarioRepository.count());
    }

    @Test
    void gestorNaoCadastraOutroGestorNemPaciente() throws Exception {
        for (String perfil : new String[] {"GESTOR", "PACIENTE"}) {
            cadastrar(Perfil.GESTOR, corpo("Pessoa Fictícia", perfil.toLowerCase() + "@exemplo.test", perfil, SENHA_INICIAL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("perfil"))
                .andExpect(jsonPath("$.mensagem").value("Papel inválido: escolha RECEPCIONISTA ou PROFISSIONAL"));
        }

        assertEquals(0, usuarioRepository.count());
    }

    @Test
    void papelInexistenteRecebe400() throws Exception {
        cadastrar(Perfil.GESTOR, corpo("Pessoa Fictícia", "admin@exemplo.test", "ADMIN", SENHA_INICIAL))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem").value("Dados inválidos"));

        assertEquals(0, usuarioRepository.count());
    }

    @Test
    void camposObrigatoriosEEmailInvalidoRecebem400() throws Exception {
        cadastrar(Perfil.GESTOR, corpo(" ", "nome@exemplo.test", "PROFISSIONAL", SENHA_INICIAL))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("nome"))
            .andExpect(jsonPath("$.mensagem").value("Nome é obrigatório"));

        cadastrar(Perfil.GESTOR, corpo("Pessoa Fictícia", "sem-arroba", "PROFISSIONAL", SENHA_INICIAL))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("email"))
            .andExpect(jsonPath("$.mensagem").value("E-mail inválido"));

        cadastrar(Perfil.GESTOR, """
                {"nome":"Pessoa Fictícia","email":"sempapel@exemplo.test","cpf":"529.982.247-25",
                 "cartaoSus":"898001234567891","senhaInicial":"inicial123"}
                """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("perfil"))
            .andExpect(jsonPath("$.mensagem").value("Papel é obrigatório"));

        assertEquals(0, usuarioRepository.count());
    }

    // Controle de acesso: rota só do GESTOR

    @Test
    void outrosPerfisRecebem403ENadaEGravado() throws Exception {
        for (Perfil perfil : new Perfil[] {Perfil.PACIENTE, Perfil.RECEPCIONISTA, Perfil.PROFISSIONAL}) {
            cadastrar(perfil, corpo("Pessoa Fictícia", "negado@exemplo.test", "PROFISSIONAL", SENHA_INICIAL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Acesso não permitido"));
        }

        assertEquals(0, usuarioRepository.count());
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mockMvc.perform(post(ROTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("Pessoa Fictícia", "anonimo@exemplo.test", "PROFISSIONAL", SENHA_INICIAL)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensagem").value("Não autenticado"));

        assertEquals(0, usuarioRepository.count());
    }

    private ResultActions cadastrar(Perfil perfilLogado, String corpo) throws Exception {
        return mockMvc.perform(post(ROTA)
            .header("Authorization", "Bearer " + token(perfilLogado))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    private String corpo(String nome, String email, String perfil, String senhaInicial) {
        return corpo(nome, email, CPF, perfil, senhaInicial);
    }

    private String corpo(String nome, String email, String cpf, String perfil, String senhaInicial) {
        return corpo(nome, email, cpf, CARTAO_SUS, perfil, senhaInicial);
    }

    private String corpo(String nome, String email, String cpf, String cartaoSus, String perfil, String senhaInicial) {
        return """
            {"nome":"%s","email":"%s","cpf":"%s","cartaoSus":"%s","perfil":"%s","senhaInicial":"%s"}
            """.formatted(nome, email, cpf, cartaoSus, perfil, senhaInicial);
    }

    private void salvarExistente(String email, String cpf) {
        String cpfLimpo = cpf.replaceAll("\\D", "");
        usuarioRepository.save(new Usuario("Usuária Existente", email, protecaoCpf.hash(cpfLimpo),
            protecaoCpf.cifrar(cpfLimpo), "hash", Perfil.RECEPCIONISTA));
    }

    /** Token assinado sem gravar o usuário logado, como nos outros testes de controller. */
    private String token(Perfil perfil) {
        Usuario usuario = new Usuario("Usuário de Teste", perfil.name().toLowerCase() + ".logado@exemplo.test",
            "hash-cpf-" + perfil, "cpf-cifrado-de-teste", "hash-senha-de-teste", perfil);
        return jwtService.gerarToken(usuario);
    }
}
