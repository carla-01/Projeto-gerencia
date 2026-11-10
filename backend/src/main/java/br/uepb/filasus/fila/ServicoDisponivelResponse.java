package br.uepb.filasus.fila;

/** Serviço na tela de escolha do paciente (US-06). "aberta" = dentro do horário e com senhas disponíveis. */
public record ServicoDisponivelResponse(Long id, String nome, boolean aberta) {
}
