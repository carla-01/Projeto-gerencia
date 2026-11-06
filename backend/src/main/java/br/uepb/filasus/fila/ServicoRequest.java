package br.uepb.filasus.fila;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServicoRequest(
    @NotBlank(message = "Nome do serviço é obrigatório")
    @Size(max = 40, message = "Nome do serviço deve ter no máximo 40 caracteres")
    String nome,

    @NotNull(message = "Horário de abertura é obrigatório")
    @JsonFormat(pattern = "HH:mm")
    LocalTime horarioAbertura,

    @NotNull(message = "Horário de fechamento é obrigatório")
    @JsonFormat(pattern = "HH:mm")
    LocalTime horarioFechamento
) {
}
