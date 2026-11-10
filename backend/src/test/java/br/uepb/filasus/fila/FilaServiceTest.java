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
import java.time.ZoneId;
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
    private static final ZoneId FUSO_UBS = ZoneId.of("America/Fortaleza");
    private static final Clock RELOGIO = Clock.fixed(
        Instant.parse("2026-11-09T12:00:00Z"), FUSO_UBS);

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
    void converteDozeHorasUtcParaNoveHorasNoHorarioDaUbs() {
        configurar(7, 17, 10);
        when(senhaRepository.countByServicoAndDataRef(servico, HOJE)).thenReturn(9L);
        Clock meioDiaUtc = Clock.fixed(Instant.parse("2026-11-09T12:00:00Z"), FUSO_UBS);

        assertTrue(filaService(meioDiaUtc).estaAberta(SERVICO_ID));
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
            Instant.parse("2026-11-09T09:59:00Z"), FUSO_UBS);

        assertFalse(filaService(antesDaAbertura).estaAberta(SERVICO_ID));
        verify(senhaRepository, never()).countByServicoAndDataRef(servico, HOJE);
    }

    @Test
    void consideraAberturaInclusivaEFechamentoExclusivo() {
        configurar(7, 17, 10);
        when(senhaRepository.countByServicoAndDataRef(servico, HOJE)).thenReturn(0L);

        Clock horarioAbertura = Clock.fixed(
            Instant.parse("2026-11-09T10:00:00Z"), FUSO_UBS);
        assertTrue(filaService(horarioAbertura).estaAberta(SERVICO_ID));

        Clock horarioFechamento = Clock.fixed(
            Instant.parse("2026-11-09T20:00:00Z"), FUSO_UBS);
        assertFalse(filaService(horarioFechamento).estaAberta(SERVICO_ID));
    }

    @Test
    void usaDataLocalDaUbsNaContagemDoLimite() {
        configurar(7, 22, 10);
        LocalDate diaLocal = LocalDate.of(2026, 11, 9);
        when(senhaRepository.countByServicoAndDataRef(servico, diaLocal)).thenReturn(9L);
        Clock depoisDaMeiaNoiteUtc = Clock.fixed(
            Instant.parse("2026-11-10T00:30:00Z"), FUSO_UBS);

        assertTrue(filaService(depoisDaMeiaNoiteUtc).estaAberta(SERVICO_ID));
        verify(senhaRepository).countByServicoAndDataRef(servico, diaLocal);
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
