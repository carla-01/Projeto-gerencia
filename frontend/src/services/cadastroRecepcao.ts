/**
 * Cadastro de paciente pela recepção (US05 - SCRUM-40).
 *
 * Chama o POST /api/recepcao/pacientes do back-end com o token da recepcionista.
 */
import { obterToken } from './auth'
import { somenteNumeros } from './cpf'

export type PrioridadeRecepcao = 'NENHUMA' | 'IDOSO' | 'GESTANTE' | 'PCD'

export type DadosCadastroRecepcao = {
  nome: string
  cpf: string
  cartaoSus: string
  telefone: string
  prioridade: PrioridadeRecepcao
  consentimentoVerbal: boolean
}

export type CampoCadastroRecepcao = keyof DadosCadastroRecepcao

/**
 * Resposta do back-end. jaCadastrado = true quando o CPF já existia:
 * nesse caso vem o cadastro existente, e nada novo foi criado (CT-US05-02).
 */
export type PacienteCadastrado = {
  pacienteId: number
  nome: string
  cpfMascarado: string
  prioridade: PrioridadeRecepcao
  cadastradoEm: string
  jaCadastrado: boolean
  versaoTermo: string | null
  consentimentoEm: string | null
}

/** O que a tela deve fazer com o erro. */
export type TipoErro = 'dados' | 'sem-login' | 'sem-permissao' | 'servidor'

/** Erro com a mensagem que deve aparecer na tela e, quando houver, o campo com problema. */
export class ErroCadastroRecepcao extends Error {
  tipo: TipoErro
  campo: CampoCadastroRecepcao | null

  constructor(tipo: TipoErro, mensagem: string, campo: CampoCadastroRecepcao | null) {
    super(mensagem)
    this.tipo = tipo
    this.campo = campo
  }
}

/** Envia o cadastro. CPF, cartão SUS e telefone vão só com os números. */
export async function cadastrarPaciente(dados: DadosCadastroRecepcao): Promise<PacienteCadastrado> {
  let telefone: string | null = somenteNumeros(dados.telefone)
  if (telefone === '') {
    telefone = null
  }

  const corpo = {
    nome: dados.nome.trim(),
    cpf: somenteNumeros(dados.cpf),
    cartaoSus: somenteNumeros(dados.cartaoSus),
    telefone: telefone,
    prioridade: dados.prioridade,
    consentimentoVerbal: dados.consentimentoVerbal,
  }

  let resposta: Response
  try {
    resposta = await fetch('/api/recepcao/pacientes', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + obterToken(),
      },
      body: JSON.stringify(corpo),
    })
  } catch {
    throw new ErroCadastroRecepcao('servidor', 'Não foi possível conectar ao servidor. Tente de novo.', null)
  }

  // 201 = paciente criado; 200 = CPF já existia e veio o cadastro existente.
  if (resposta.status === 201 || resposta.status === 200) {
    return resposta.json()
  }

  if (resposta.status === 400) {
    // O back-end manda { "campo": "cpf", "mensagem": "CPF inválido" }.
    const erro = await resposta.json()
    throw new ErroCadastroRecepcao('dados', erro.mensagem, erro.campo)
  }

  if (resposta.status === 401) {
    throw new ErroCadastroRecepcao('sem-login', 'Sua sessão expirou. Entre de novo.', null)
  }

  if (resposta.status === 403) {
    throw new ErroCadastroRecepcao('sem-permissao', 'Acesso não permitido', null)
  }

  if (resposta.status === 409) {
    throw new ErroCadastroRecepcao('dados', 'Este CPF acabou de ser cadastrado. Tente de novo para abrir o cadastro.', 'cpf')
  }

  throw new ErroCadastroRecepcao('servidor', 'Não foi possível concluir o cadastro agora. Tente de novo.', null)
}
