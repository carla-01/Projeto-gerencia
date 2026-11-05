package br.uepb.filasus.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import br.uepb.filasus.auth.JwtService;
import br.uepb.filasus.config.JwtAccessDeniedHandler;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;

/** Passos da US02 - Controle de acesso por perfil (SCRUM-38). */
public class ControleDeAcessoSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    // Guarda em memória o que o JwtAccessDeniedHandler escreve no log durante o cenário.
    private final Logger logDoAcessoNegado = (Logger) LoggerFactory.getLogger(JwtAccessDeniedHandler.class);
    private final ListAppender<ILoggingEvent> logCapturado = new ListAppender<>();

    private String token;
    private MvcResult resposta;

    @Before
    public void capturarLog() {
        logCapturado.start();
        logDoAcessoNegado.addAppender(logCapturado);
    }

    @After
    public void pararDeCapturarLog() {
        logDoAcessoNegado.detachAppender(logCapturado);
        logCapturado.stop();
    }

    @Dado("que estou logado como {word}")
    public void queEstouLogadoComo(String perfil) {
        Perfil perfilDoUsuario = Perfil.valueOf(perfil.toUpperCase());
        Usuario usuario = new Usuario("Usuário de Teste", perfil + "@filasus.com",
            "cpf-hash", "cpf-cifrado", "hash", perfilDoUsuario);
        token = jwtService.gerarToken(usuario);
    }

    @Quando("tento acessar a rota do painel da recepção {string}")
    public void tentoAcessarARota(String rota) throws Exception {
        resposta = mockMvc.perform(get(rota)
                .header("Authorization", "Bearer " + token))
            .andReturn();
    }

    @Entao("o sistema responde {int}")
    public void oSistemaResponde(int status) {
        assertThat(resposta.getResponse().getStatus()).isEqualTo(status);
    }

    @E("o log registra {string} com o perfil {string}, o método {string} e a rota {string}")
    public void oLogRegistra(String mensagem, String perfil, String metodo, String rota) {
        assertThat(linhasDoLog()).anySatisfy(linha -> assertThat(linha)
            .contains(mensagem)
            .contains("perfil=" + perfil)
            .contains("metodo=" + metodo)
            .contains("rota=" + rota));
    }

    @E("o log não mostra o token de acesso")
    public void oLogNaoMostraOToken() {
        assertThat(linhasDoLog()).noneMatch(linha -> linha.contains(token));
    }

    private List<String> linhasDoLog() {
        return logCapturado.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }
}
