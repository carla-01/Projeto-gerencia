package br.uepb.filasus.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Relógio simulado só para os testes: o horário fica parado e só anda
 * quando o teste chama avancar(). Assim dá para testar o bloqueio de
 * 15 minutos sem esperar 15 minutos de verdade.
 */
public class RelogioAjustavel extends Clock {

    private Instant agora;

    public RelogioAjustavel(Instant inicio) {
        this.agora = inicio;
    }

    public void definir(Instant novoHorario) {
        this.agora = novoHorario;
    }

    public void avancar(Duration tempo) {
        this.agora = this.agora.plus(tempo);
    }

    @Override
    public Instant instant() {
        return agora;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        // Os testes sempre usam UTC.
        return this;
    }
}
