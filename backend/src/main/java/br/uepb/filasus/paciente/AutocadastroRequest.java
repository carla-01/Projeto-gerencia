package br.uepb.filasus.paciente;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST /pacientes/autocadastro (US-04 - SCRUM-39).
 *
 * O CPF pode vir com ou sem máscara. A prioridade é opcional (padrão NENHUMA).
 * O e-mail é obrigatório porque também serve de login.
 * aceiteTermo precisa ser true (CT-US04-02).
 *
 * Exemplo:
 * { "nome": "Maria da Silva", "cpf": "529.982.247-25", "cartaoSus": "898001234567890",
 *   "telefone": "83999990000", "email": "maria@email.com", "senha": "minhasenha",
 *   "prioridade": "GESTANTE", "aceiteTermo": true }
 */
public record AutocadastroRequest(
    @NotBlank(message = "Informe seu nome") @Size(max = 120) String nome,
    @NotBlank(message = "Informe seu CPF") String cpf,
    @NotBlank(message = "Informe o número do cartão SUS")
    @Pattern(regexp = "\\d{15}", message = "O cartão SUS tem 15 números") String cartaoSus,
    @Size(max = 20) String telefone,
    @NotBlank(message = "Informe seu e-mail") @Email(message = "E-mail inválido") @Size(max = 160) String email,
    // O bcrypt só usa os primeiros 72 bytes da senha.
    @NotBlank(message = "Crie uma senha")
    @Size(min = 8, max = 72, message = "A senha precisa ter de 8 a 72 caracteres") String senha,
    Prioridade prioridade,
    @AssertTrue(message = PacienteService.FINALIDADE_DOS_DADOS) boolean aceiteTermo
) {
}
