/**
 * Autocadastro do paciente (US04 - SCRUM-39).
 *
 * Chama o POST /pacientes/autocadastro do back-end. A rota é pública:
 * quem está se cadastrando ainda não tem login.
 */

export type Prioridade = 'NENHUMA' | 'IDOSO' | 'GESTANTE' | 'PCD'

export type DadosAutocadastro = {
  nome: string
  cpf: string
  cartaoSus: string
  telefone: string
  email: string
  senha: string
  prioridade: Prioridade
  aceiteTermo: boolean
}

export type CampoDoCadastro = keyof DadosAutocadastro

export type CadastroConcluido = {
  pacienteId: number
  nome: string
  versaoTermo: string
  aceitoEm: string
}

/**
 * Erro com a mensagem que deve aparecer na tela.
 * Quando o back-end diz qual campo está errado, vem em "campo".
 */
export class ErroAutocadastro extends Error {
  campo: CampoDoCadastro | null

  constructor(mensagem: string, campo: CampoDoCadastro | null = null) {
    super(mensagem)
    this.campo = campo
  }
}

/**
 * Envia o cadastro. Se der certo, devolve o registro do aceite.
 * O CPF, o telefone e o cartão SUS vão só com os números.
 */
export async function autocadastrar(dados: DadosAutocadastro): Promise<CadastroConcluido> {
  const telefone = dados.telefone.replace(/\D/g, '')
  const corpo = {
    nome: dados.nome.trim(),
    cpf: dados.cpf.replace(/\D/g, ''),
    cartaoSus: dados.cartaoSus.replace(/\D/g, ''),
    telefone: telefone === '' ? null : telefone,
    email: dados.email.trim(),
    senha: dados.senha,
    prioridade: dados.prioridade,
    aceiteTermo: dados.aceiteTermo,
  }

  let resposta: Response
  try {
    resposta = await fetch('/pacientes/autocadastro', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(corpo),
    })
  } catch {
    throw new ErroAutocadastro('Não foi possível conectar ao servidor. Tente de novo.')
  }

  if (resposta.status === 400 || resposta.status === 409) {
    // O back-end manda { "campo": "cpf", "mensagem": "CPF inválido" } ou só { "mensagem": "..." }.
    const erro = await resposta.json().catch(() => null)
    throw new ErroAutocadastro(
      erro?.mensagem ?? 'Confira os dados e tente de novo.',
      erro?.campo ?? null,
    )
  }

  if (!resposta.ok) {
    throw new ErroAutocadastro('Não foi possível concluir o cadastro agora. Tente de novo.')
  }

  return resposta.json()
}
