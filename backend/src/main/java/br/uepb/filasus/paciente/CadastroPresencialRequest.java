package br.uepb.filasus.paciente;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST /api/recepcao/pacientes (US-05 - SCRUM-40).
 *
 * A recepcionista cadastra quem não tem celular ou não sabe usar o sistema.
 * Por isso não tem e-mail nem senha: esse paciente não faz login.
 * O telefone e a prioridade são opcionais (prioridade padrão NENHUMA).
 * consentimentoVerbal precisa ser true: a recepcionista leu (ou explicou) o termo
 * e o paciente concordou em voz alta.
 *
 * Exemplo:
 * { "nome": "Antônio Lima", "cpf": "529.982.247-25", "cartaoSus": "700123456789012",
 *   "telefone": "8333330000", "prioridade": "IDOSO", "consentimentoVerbal": true }
 */
public record CadastroPresencialRequest(
    @NotBlank(message = "Informe o nome do paciente") @Size(max = 120) String nome,
    @NotBlank(message = "Informe o CPF do paciente") String cpf,
    @NotBlank(message = "Informe o número do cartão SUS")
    @Pattern(regexp = "\\d{15}", message = "O cartão SUS tem 15 números") String cartaoSus,
    @Size(max = 20) String telefone,
    Prioridade prioridade,
    @AssertTrue(message = PacienteService.CONSENTIMENTO_VERBAL_OBRIGATORIO) boolean consentimentoVerbal
) {
}
