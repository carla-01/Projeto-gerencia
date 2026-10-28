/**
 * Regras do CPF usadas nos formulários: máscara e dígitos verificadores.
 * O back-end confere de novo; aqui é só para avisar o erro antes de enviar.
 */

/** Deixa só os números: "529.982.247-25" vira "52998224725". */
export function somenteNumeros(texto: string): string {
  return texto.replace(/\D/g, '')
}

/** Coloca a máscara 000.000.000-00 enquanto a pessoa digita. */
export function mascararCpf(texto: string): string {
  const numeros = somenteNumeros(texto).slice(0, 11)
  return numeros
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2')
}

/** Calcula um dígito verificador usando os "quantidade" primeiros números. */
function calcularDigito(cpf: string, quantidade: number): number {
  let soma = 0
  let peso = quantidade + 1
  for (let i = 0; i < quantidade; i++) {
    soma += Number(cpf[i]) * peso
    peso--
  }
  const resto = (soma * 10) % 11
  if (resto === 10) {
    return 0
  }
  return resto
}

/** Confere os 11 números e os dois dígitos verificadores. */
export function cpfValido(texto: string): boolean {
  const cpf = somenteNumeros(texto)
  if (cpf.length !== 11) {
    return false
  }
  // 000.000.000-00, 111.111.111-11... passam na conta, mas não existem.
  if (/^(\d)\1{10}$/.test(cpf)) {
    return false
  }
  return calcularDigito(cpf, 9) === Number(cpf[9]) && calcularDigito(cpf, 10) === Number(cpf[10])
}
