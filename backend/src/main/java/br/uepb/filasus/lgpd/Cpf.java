package br.uepb.filasus.lgpd;

/**
 * Regras do CPF: tirar a máscara e conferir os dígitos verificadores.
 */
public final class Cpf {

    private Cpf() {
    }

    /** "123.456.789-09" vira "12345678909". Espaços nas pontas também saem. */
    public static String normalizar(String cpf) {
        if (cpf == null) {
            return "";
        }
        return cpf.trim().replace(".", "").replace("-", "");
    }

    /**
     * Esconde o meio do CPF (já normalizado) para mostrar na tela:
     * "52998224725" vira "529.***.***-25" (protótipo R2).
     */
    public static String mascarar(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return "";
        }
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
    }

    /**
     * Confere se o CPF (já normalizado) tem 11 números e os dois dígitos
     * verificadores certos. CPFs com todos os números iguais são inválidos.
     */
    public static boolean ehValido(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}")) {
            return false;
        }

        if (cpf.chars().distinct().count() == 1) {
            return false;
        }

        int primeiroDigito = calcularDigito(cpf, 9);
        int segundoDigito = calcularDigito(cpf, 10);

        return primeiroDigito == cpf.charAt(9) - '0'
            && segundoDigito == cpf.charAt(10) - '0';
    }

    /** Calcula o dígito verificador usando os "quantidade" primeiros números. */
    private static int calcularDigito(String cpf, int quantidade) {
        int soma = 0;
        int peso = quantidade + 1;

        for (int i = 0; i < quantidade; i++) {
            soma += (cpf.charAt(i) - '0') * peso;
            peso--;
        }

        int resto = (soma * 10) % 11;
        if (resto == 10) {
            return 0;
        }
        return resto;
    }
}
