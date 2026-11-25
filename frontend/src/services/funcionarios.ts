import { obterToken } from './auth'
import type { Perfil } from './auth'

/**
 * Cadastro e lista de funcionários pelo gestor (US03 - SCRUM-152).
 * Usa as rotas do back-end das subtarefas SCRUM-150 e SCRUM-151.
 */

/** Papéis que o gestor pode cadastrar (regra da PO). */
export type PapelFuncionario = 'RECEPCIONISTA' | 'PROFISSIONAL'

export type Funcionario = {
  id: number
  nome: string
  email: string
  perfil: Perfil
  ativo: boolean
  setorId: number | null
  setor: string | null
}

/** Serviço da UBS que pode ser o setor do profissional. */
export type Setor = {
  id: number
  nome: string
}

export type DadosFuncionario = {
  nome: string
  email: string
  cpf: string
  cartaoSus: string
  perfil: PapelFuncionario
  servicoId: number | null
  senhaInicial: string
}

/** Erro com a mensagem para a tela; "campo" diz qual campo do formulário marcar, quando o back-end informa. */
export class ErroFuncionario extends Error {
  campo: string | null

  constructor(mensagem: string, campo: string | null = null) {
    super(mensagem)
    this.campo = campo
  }
}

const URL_FUNCIONARIOS = '/api/gestor/funcionarios'
const URL_SETORES = '/api/gestor/servicos'

export async function listarFuncionarios(): Promise<Funcionario[]> {
  return buscarLista<Funcionario>(URL_FUNCIONARIOS, 'Não foi possível carregar a lista de usuários. Tente de novo.')
}

export async function listarSetores(): Promise<Setor[]> {
  return buscarLista<Setor>(URL_SETORES, 'Não foi possível carregar os setores. Tente de novo.')
}

/** Cadastra o funcionário. Se der errado, lança ErroFuncionario com a mensagem do back-end. */
export async function cadastrarFuncionario(dados: DadosFuncionario): Promise<Funcionario> {
  const resposta = await chamar(URL_FUNCIONARIOS, {
    method: 'POST',
    body: JSON.stringify(dados),
  })

  if (resposta.ok) {
    return resposta.json()
  }
  if (resposta.status === 400 || resposta.status === 409) {
    // Ex.: 409 "E-mail já cadastrado" (Cenário 2) ou 400 "CPF inválido" com campo "cpf".
    const corpo = await lerCorpo(resposta)
    if (corpo !== null) {
      throw new ErroFuncionario(corpo.mensagem, corpo.campo)
    }
  }
  throw new ErroFuncionario('Não foi possível cadastrar o funcionário agora. Tente de novo.')
}

async function buscarLista<T>(url: string, mensagemDeErro: string): Promise<T[]> {
  const resposta = await chamar(url, { method: 'GET' })
  if (!resposta.ok) {
    throw new ErroFuncionario(mensagemDeErro)
  }
  const corpo = await resposta.json()
  if (!Array.isArray(corpo)) {
    throw new ErroFuncionario(mensagemDeErro)
  }
  return corpo
}

/** Faz a chamada com o token do gestor e trata sessão expirada e acesso negado. */
async function chamar(url: string, opcoes: RequestInit): Promise<Response> {
  const token = obterToken()
  if (token === null) {
    throw new ErroFuncionario('Sua sessão expirou. Entre de novo.')
  }

  let resposta: Response
  try {
    resposta = await fetch(url, {
      ...opcoes,
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
    })
  } catch {
    throw new ErroFuncionario('Não foi possível conectar ao servidor. Tente de novo.')
  }

  if (resposta.status === 401) {
    throw new ErroFuncionario('Sua sessão expirou. Entre de novo.')
  }
  if (resposta.status === 403) {
    throw new ErroFuncionario('Acesso não permitido')
  }
  return resposta
}

async function lerCorpo(resposta: Response): Promise<{ mensagem: string; campo: string | null } | null> {
  try {
    const corpo = await resposta.json()
    if (typeof corpo?.mensagem !== 'string') {
      return null
    }
    return { mensagem: corpo.mensagem, campo: typeof corpo.campo === 'string' ? corpo.campo : null }
  } catch {
    return null
  }
}
