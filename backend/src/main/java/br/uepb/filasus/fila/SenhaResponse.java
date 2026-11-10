package br.uepb.filasus.fila;

import java.time.LocalDate;

/** Senha gerada para o paciente (US-06). A posição na fila entra com a US-07. */
public record SenhaResponse(Long id, int numero, String servico, LocalDate data, StatusSenha status) {

    public SenhaResponse(Senha senha) {
        this(senha.getId(), senha.getNumero(), senha.getServico().getNome(), senha.getDataRef(), senha.getStatus());
    }
}
