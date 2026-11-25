package br.uepb.filasus.fila;

import java.util.Comparator;

import br.uepb.filasus.paciente.Prioridade;

/**
 * Ordem de atendimento da fila com a prioridade legal (US-11 - SCRUM-159).
 *
 * Regra da PO (Planning de 17/11): urgência primeiro; depois idoso, gestante e pessoa com
 * deficiência, todos no mesmo nível; depois quem não tem prioridade. Dentro do mesmo nível,
 * vale a ordem de chegada (número da senha).
 *
 * É a mesma ordem da posição do paciente (US-07) e da chamada do próximo (US-13).
 */
public final class OrdemFila {

    /** Senha que vem antes na fila fica antes na lista ordenada. */
    public static final Comparator<Senha> ORDEM = Comparator
        .comparingInt((Senha senha) -> nivel(senha.getPrioridadeAplicada()))
        .thenComparingInt(Senha::getNumero);

    private OrdemFila() {
    }

    /** Nível da prioridade: quanto menor, mais cedo a pessoa é atendida. */
    static int nivel(Prioridade prioridade) {
        switch (prioridade) {
            case URGENCIA:
                return 0;
            case IDOSO:
            case GESTANTE:
            case PCD:
                return 1;
            default:
                return 2;
        }
    }
}
