package br.uepb.filasus.usuario;

/**
 * Perfis de acesso do FilaSUS (US01 - SCRUM-37).
 *
 * Cada usuário tem exatamente um perfil, e é ele que decide
 * quais telas o usuário pode ver depois do login.
 */
public enum Perfil {
    PACIENTE,
    RECEPCIONISTA,
    PROFISSIONAL,
    GESTOR
}
