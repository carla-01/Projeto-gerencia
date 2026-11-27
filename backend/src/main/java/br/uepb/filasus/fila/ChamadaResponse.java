package br.uepb.filasus.fila;

/**
 * Resultado do "Chamar próximo" (US-13 - SCRUM-162).
 *
 * Quando alguém é chamado, "senha" vem preenchida e "mensagem" vem nula.
 * Com a fila vazia, "senha" vem nula e "mensagem" explica.
 */
public record ChamadaResponse(String mensagem, SenhaChamadaResponse senha) {
}
