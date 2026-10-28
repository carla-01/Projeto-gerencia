package br.uepb.filasus.paciente;

/**
 * Prioridade legal do paciente (US-04, US-05 e US-11).
 *
 * Os mesmos valores aceitos pelo CHECK da coluna paciente.prioridade
 * em docs/devops/banco/02-criar-tabelas.sql.
 */
public enum Prioridade {
    NENHUMA,
    IDOSO,
    GESTANTE,
    PCD,
    URGENCIA
}
