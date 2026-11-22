package br.uepb.filasus.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados que o gestor informa para cadastrar um funcionário (US03 - SCRUM-15).
 *
 * O CPF é obrigatório porque a tabela usuario do DER exige cpf_hash e cpf_cifrado.
 *
 * Regras da PO (refinamento de 16/11): senha inicial com no mínimo 8 caracteres,
 * sem envio de e-mail; o perfil só pode ser RECEPCIONISTA ou PROFISSIONAL
 * (conferido no FuncionarioService).
 */
public record CadastroFuncionarioRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
    String nome,

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 160, message = "E-mail deve ter no máximo 160 caracteres")
    String email,

    @NotBlank(message = "CPF é obrigatório")
    String cpf,

    @NotBlank(message = "Cartão SUS é obrigatório")
    @Pattern(regexp = "\\d{15}", message = "O cartão SUS tem 15 números")
    String cartaoSus,

    @NotNull(message = "Papel é obrigatório")
    Perfil perfil,

    @NotBlank(message = "Senha inicial é obrigatória")
    @Size(min = 8, max = 72, message = "Senha inicial deve ter entre 8 e 72 caracteres")
    String senhaInicial
) {

    /** A senha, o CPF e o cartão SUS nunca aparecem em log nem em mensagem de erro. */
    @Override
    public String toString() {
        return "CadastroFuncionarioRequest[nome=" + nome + ", email=" + email + ", perfil=" + perfil + "]";
    }
}
