package br.uepb.filasus.fila;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FilaServiceTest {

    private static final Long SERVICO_ID = 10L;
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 9);
    private static final Clock RELOGIO = Clock.fixed(
        Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ConfiguracaoServicoRepository configuracaoServicoRepository;

    @Mock
    private SenhaRepository senhaRepository;

    private Servico servico;

    @BeforeEach
    void configurarServico() {
        servico = new Servico("Clínico");
    }

    @Test
    void ficaAbertaDentroDoHorarioEnquantoHaLimiteDisponivel() {
        configurar(7, 17, 10);
        when(senhaRepository.countByServicoAndDataRef(servico, HOJE)).thenReturn(9L);

        assertTrue(filaService(RELOGIO).estaAberta(SERVICO_ID));
    }

    @Test
    void fechaQuandoAtingeOLimiteDiario() {
        configurar(7, 17, 10);
        when(senhaRepository.countByServicoAndDataRef(servico, HOJE)).thenReturn(10L);

        assertFalse(filaService(RELOGIO).estaAberta(SERVICO_ID));
    }

    @Test
    void ficaFechadaForaDoHorarioDeFuncionamento() {
        configurar(7, 17, 10);
        Clock antesDaAbertura = Clock.fixed(
            Instant.parse("2026-11-09T06:59:00Z"), ZoneOffset.UTC);

        assertFalse(filaService(antesDaAbertura).estaAberta(SERVICO_ID));
        verify(senhaRepository, never()).countByServicoAndDataRef(servico, HOJE);
    }

    @Test
    void consideraAberturaInclusivaEFechamentoExclusivo() {
        configurar(7, 17, 10);
        when(senhaRepository.countByServicoAndDataRef(servico, HOJE)).thenReturn(0L);

        Clock horarioAbertura = Clock.fixed(
            Instant.parse("2026-11-09T07:00:00Z"), ZoneOffset.UTC);
        assertTrue(filaService(horarioAbertura).estaAberta(SERVICO_ID));

        Clock horarioFechamento = Clock.fixed(
            Instant.parse("2026-11-09T17:00:00Z"), ZoneOffset.UTC);
        assertFalse(filaService(horarioFechamento).estaAberta(SERVICO_ID));
    }

    @Test
    void falhaExplicitamenteQuandoServicoNaoTemConfiguracao() {
        when(configuracaoServicoRepository.findByServico_Id(SERVICO_ID)).thenReturn(Optional.empty());

        assertThrows(ConfiguracaoServicoAusenteException.class,
            () -> filaService(RELOGIO).estaAberta(SERVICO_ID));
    }

    private FilaService filaService(Clock clock) {
        return new FilaService(configuracaoServicoRepository, senhaRepository, clock);
    }

    private void configurar(int abertura, int fechamento, int limite) {
        ConfiguracaoServico configuracao = new ConfiguracaoServico(servico,
            LocalTime.of(abertura, 0), LocalTime.of(fechamento, 0), limite);
        when(configuracaoServicoRepository.findByServico_Id(SERVICO_ID))
            .thenReturn(Optional.of(configuracao));
    }
}
