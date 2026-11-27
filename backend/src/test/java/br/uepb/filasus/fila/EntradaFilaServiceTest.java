package br.uepb.filasus.fila;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import br.uepb.filasus.paciente.Paciente;
import br.uepb.filasus.paciente.PacienteRepository;
import br.uepb.filasus.paciente.Prioridade;

/** Nova tentativa quando dois pacientes geram o mesmo número ao mesmo tempo (DA-DES-I2-01). */
@ExtendWith(MockitoExtension.class)
class EntradaFilaServiceTest {

    private static final Long USUARIO_ID = 1L;
    private static final Long SERVICO_ID = 10L;
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);
    private static final Clock RELOGIO = Clock.fixed(
        Instant.parse("2026-11-10T12:00:00Z"), ZoneId.of("America/Fortaleza"));

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private SenhaRepository senhaRepository;

    @Mock
    private ConfiguracaoServicoRepository configuracaoServicoRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private EntradaFilaService entradaFilaService;
    private Servico servico;
    private Paciente paciente;

    @BeforeEach
    void prepararFilaAberta() {
        // FilaService de verdade, com os repositórios simulados: fila das 07:00 às 17:00 e 40 senhas.
        FilaService filaService = new FilaService(configuracaoServicoRepository, senhaRepository, RELOGIO);
        entradaFilaService = new EntradaFilaService(pacienteRepository, servicoRepository,
            senhaRepository, filaService, RELOGIO, transactionManager);
        servico = new Servico("Clínico");
        paciente = new Paciente("Paciente Fictício", "cpf-hash", "cpf-cifrado", Prioridade.NENHUMA, Instant.now());

        when(pacienteRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(paciente));
        when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico));
        when(configuracaoServicoRepository.findByServico_Id(any())).thenReturn(Optional.of(
            new ConfiguracaoServico(servico, LocalTime.of(7, 0), LocalTime.of(17, 0), 40)));
        when(senhaRepository.countByServicoAndDataRef(servico, HOJE)).thenReturn(0L);
        when(senhaRepository.buscarSenhaAtiva(paciente, servico, HOJE)).thenReturn(Optional.empty());
    }

    @Test
    void tentaDeNovoComOProximoNumeroQuandoONumeroRepete() {
        // Na primeira tentativa outro paciente já gravou a senha 3; na segunda o maior já é 3.
        when(senhaRepository.maiorNumeroDoDia(servico, HOJE)).thenReturn(2, 3);
        when(senhaRepository.saveAndFlush(any(Senha.class)))
            .thenThrow(new DataIntegrityViolationException("uk_senha_servico_dia_numero"))
            .thenAnswer(chamada -> chamada.getArgument(0));

        SenhaResponse senha = entradaFilaService.entrar(USUARIO_ID, SERVICO_ID);

        assertEquals(4, senha.numero());
        verify(senhaRepository, times(2)).saveAndFlush(any(Senha.class));
    }

    @Test
    void desisteDepoisDeTresTentativas() {
        when(senhaRepository.maiorNumeroDoDia(servico, HOJE)).thenReturn(1);
        when(senhaRepository.saveAndFlush(any(Senha.class)))
            .thenThrow(new DataIntegrityViolationException("uk_senha_servico_dia_numero"));

        assertThrows(DataIntegrityViolationException.class,
            () -> entradaFilaService.entrar(USUARIO_ID, SERVICO_ID));
        verify(senhaRepository, times(EntradaFilaService.TENTATIVAS)).saveAndFlush(any(Senha.class));
    }
}
