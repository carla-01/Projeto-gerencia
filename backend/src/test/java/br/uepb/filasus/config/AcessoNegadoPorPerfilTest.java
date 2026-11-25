package br.uepb.filasus.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.uepb.filasus.auth.JwtService;
import br.uepb.filasus.fila.ConfiguracaoServicoRepository;
import br.uepb.filasus.fila.SenhaRepository;
import br.uepb.filasus.fila.ServicoRepository;
import br.uepb.filasus.paciente.ConsentimentoRepository;
import br.uepb.filasus.paciente.PacienteRepository;
import br.uepb.filasus.usuario.Perfil;
import br.uepb.filasus.usuario.Usuario;
import br.uepb.filasus.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AcessoNegadoPorPerfilTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ConsentimentoRepository consentimentoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ConfiguracaoServicoRepository configuracaoServicoRepository;

    @Autowired
    private SenhaRepository senhaRepository;
