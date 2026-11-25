package br.uepb.filasus.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.uepb.filasus.auth.JwtService;
import br.uepb.filasus.fila.Servico;
import br.uepb.filasus.fila.ServicoRepository;
import br.uepb.filasus.lgpd.ProtecaoCpf;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;

/** Passos da US03 - Gestor cadastra funcionários (SCRUM-15 / SCRUM-153). Dados fictícios. */
public class CadastrarFuncionarioSteps {

    private static final String ROTA = "/api/gestor/funcionarios";
    private static final String SENHA_INICIAL = "inicial123";
    private static final String CARTAO_SUS = "898001234567891";

    /** CPFs fictícios com dígitos verificadores válidos, um para cada pessoa do cenário. */
    private static final String CPF_GESTOR = "11144477735";
    private static final String CPF_FUNCIONARIO = "93541134780";
    private static final String CPF_EXISTENTE = "12345678909";

    private static final String EMAIL_GESTOR = "gestor.us03.bdd@exemplo.test";

    /** E-mails e setores que os cenários criam; são apagados antes e depois de cada cenário. */
    private static final List<String> EMAILS_DO_CENARIO =
        List.of(EMAIL_GESTOR, "enfermeira.bdd@exemplo.test", "repetido.bdd@exemplo.test");
    private static final List<String> SETORES_DO_CENARIO = List.of("Enfermagem BDD");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ProtecaoCpf protecaoCpf;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    private String tokenGestor;
    private String emailCadastrado;
    private String setorCadastrado;
    private long quantidadeUsuariosAntes;
    private MvcResult resposta;

    @Before("@us03")
    public void limparAntesDoCenario() {
        removerDadosDoCenario();
    }

    /**
     * Apaga o que o cenário criou, para não atrapalhar os outros testes.
     * Roda antes dos @After sem tag de outras histórias (no @After, order maior roda primeiro):
     * eles apagam todos os serviços, e o setor só sai depois que o profissional ligado a ele sai.
     */
    @After(value = "@us03", order = 20000)
    public void limparDepoisDoCenario() {
        removerDadosDoCenario();
    }

    @Dado("que estou logado como gestor da UBS")
    public void queEstouLogadoComoGestor() {
        // Salvo no banco para o token sair com o id real do usuário no "sub".
        Usuario gestor = usuarioRepository.save(new Usuario("Gestor Fictício", EMAIL_GESTOR,
            protecaoCpf.hash(CPF_GESTOR), protecaoCpf.cifrar(CPF_GESTOR), "hash", Perfil.GESTOR));
        tokenGestor = jwtService.gerarToken(gestor);
    }

    @Dado("que a UBS tem o setor {string}")
    public void queAUbsTemOSetor(String nome) {
        servicoRepository.save(new Servico(nome));
    }

    @Dado("que já existe um usuário com o e-mail {string}")
    public void queJaExisteUmUsuarioComOEmail(String email) {
        usuarioRepository.save(new Usuario("Usuária Existente", email,
            protecaoCpf.hash(CPF_EXISTENTE), protecaoCpf.cifrar(CPF_EXISTENTE), "hash", Perfil.PROFISSIONAL));
    }

    @Quando("cadastro um profissional com nome {string}, e-mail {string}, setor {string} e papel {string}")
    public void cadastroUmProfissional(String nome, String email, String setor, String papel) throws Exception {
        Servico servico = servicoRepository.findByNome(setor).orElseThrow();
        emailCadastrado = email;
        setorCadastrado = setor;
        cadastrar(corpo(nome, email, papel, servico.getId()));
    }

    @Quando("tento salvar o cadastro de uma recepcionista com o e-mail {string}")
    public void tentoSalvarOCadastro(String email) throws Exception {
        cadastrar(corpo("Recepcionista Fictícia", email, "RECEPCIONISTA", null));
    }

    @Entao("ele recebe acesso e aparece na lista de usuários")
    public void eleRecebeAcessoEApareceNaLista() throws Exception {
        assertThat(resposta.getResponse().getStatus()).isEqualTo(201);
        assertThat(usuarioRepository.count()).isEqualTo(quantidadeUsuariosAntes + 1);

        // Recebe acesso: entra com o e-mail e a senha inicial e abre o perfil de profissional.
        MvcResult login = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("login", emailCadastrado, "senha", SENHA_INICIAL))))
            .andReturn();
        assertThat(login.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(login).get("perfil").asText()).isEqualTo("PROFISSIONAL");

        // Aparece na lista de usuários, com o setor.
        MvcResult lista = mockMvc.perform(get(ROTA).header("Authorization", "Bearer " + tokenGestor)).andReturn();
        assertThat(lista.getResponse().getStatus()).isEqualTo(200);
        JsonNode funcionario = null;
        for (JsonNode item : json(lista)) {
            if (emailCadastrado.equals(item.get("email").asText())) {
                funcionario = item;
            }
        }
        assertThat(funcionario).as("o funcionário cadastrado deve aparecer na lista").isNotNull();
        assertThat(funcionario.get("perfil").asText()).isEqualTo("PROFISSIONAL");
        assertThat(funcionario.get("setor").asText()).isEqualTo(setorCadastrado);
        assertThat(funcionario.get("ativo").asBoolean()).isTrue();
    }

    @Entao("vejo {string} e nada é gravado")
    public void vejoAMensagemENadaEGravado(String mensagem) throws Exception {
        assertThat(resposta.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(resposta).get("mensagem").asText()).isEqualTo(mensagem);
        assertThat(usuarioRepository.count()).isEqualTo(quantidadeUsuariosAntes);

        // O usuário que já existia continua igual.
        Usuario existente = usuarioRepository.findByEmail("repetido.bdd@exemplo.test").orElseThrow();
        assertThat(existente.getNome()).isEqualTo("Usuária Existente");
        assertThat(existente.getPerfil()).isEqualTo(Perfil.PROFISSIONAL);
    }

    private void cadastrar(Map<String, Object> corpo) throws Exception {
        quantidadeUsuariosAntes = usuarioRepository.count();
        resposta = mockMvc.perform(post(ROTA)
                .header("Authorization", "Bearer " + tokenGestor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo)))
            .andReturn();
    }

    private Map<String, Object> corpo(String nome, String email, String papel, Long servicoId) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", nome);
        corpo.put("email", email);
        corpo.put("cpf", CPF_FUNCIONARIO);
        corpo.put("cartaoSus", CARTAO_SUS);
        corpo.put("perfil", papel);
        corpo.put("servicoId", servicoId);
        corpo.put("senhaInicial", SENHA_INICIAL);
        return corpo;
    }

    private JsonNode json(MvcResult resultado) throws Exception {
        // Sem charset no Content-Type o MockMvc leria em ISO-8859-1 e estragaria os acentos.
        return objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** Usuários antes dos setores: a ligação em profissional_servico sai junto com o usuário. */
    private void removerDadosDoCenario() {
        EMAILS_DO_CENARIO.forEach(email -> usuarioRepository.findByEmail(email).ifPresent(usuarioRepository::delete));
        SETORES_DO_CENARIO.forEach(nome -> servicoRepository.findByNome(nome).ifPresent(servicoRepository::delete));
        tokenGestor = null;
        emailCadastrado = null;
        setorCadastrado = null;
        resposta = null;
    }
}
