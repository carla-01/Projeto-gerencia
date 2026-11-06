package br.uepb.filasus.fila;

/**
 * Situação da senha na fila (US-06 a US-14).
 *
 * Os mesmos valores aceitos pelo CHECK da coluna senha.status
 * em docs/devops/banco/02-criar-tabelas.sql.
 */
public enum StatusSenha {
    /** Entrou na fila, ainda não chegou à UBS. */
    AGUARDANDO,
    /** Check-in feito pela recepção (US-10). */
    PRESENTE,
    /** O profissional chamou (US-13). */
    CHAMADA,
    /** Atendimento iniciado (US-14). */
    EM_ATENDIMENTO,
    /** Atendimento finalizado (US-14). */
    ATENDIDA,
    /** O paciente cancelou (US-08). */
    CANCELADA,
    /** Foi chamado e não apareceu (US-12). */
    FALTOSO
}
