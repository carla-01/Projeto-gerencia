/**
 * Check-in na recepção (US10 - SCRUM-156).
 *
 * Chama o POST /api/recepcao/check-in/busca e o PUT /api/recepcao/senhas/{id}/check-in
 * (SCRUM-154), o GET /api/recepcao/servicos e o POST /api/recepcao/senhas (SCRUM-155)
 * e o PUT /api/recepcao/senhas/{id}/prioridade (US11 - SCRUM-158) com o token da recepcionista.
 */
import { obterToken } from './auth'
import type { SenhaGerada, ServicoDisponivel } from './fila'

export type PrioridadeSenha = 'NENHUMA' | 'IDOSO' | 'GESTANTE' | 'PCD' | 'URGENCIA'

/** Senha mostrada no check-in. */
export type SenhaCheckIn = {
  id: number
  numero: number
  servico: string
  nomePaciente: string
  cpfMascarado: string
  status: string
  prioridade: PrioridadeSenha
  checkinEm: string | null
}

/** Senha depois de a recepção marcar a prioridade (US11). */
export type PrioridadeMarcada = {
  id: number
  numero: number
  servico: string
  status: string
  prioridade: PrioridadeSenha
  prioridadePor: number
  prioridadeEm: string
}

/** Resultado da busca. pacienteId só vem quando o CPF é de um paciente cadastrado. */
export type ResultadoBusca = {
  mensagem: string | null
  pacienteId: number | null
  nomePaciente: string | null
  senhas: SenhaCheckIn[]
}

/** O que a tela deve fazer com o erro. */
export type TipoErroCheckIn = 'aviso' | 'sem-login' | 'sem-permissao'

/** Erro com a mensagem que deve aparecer na tela. */
export class ErroCheckIn extends Error {
  tipo: TipoErroCheckIn

  constructor(tipo: TipoErroCheckIn, mensagem: string) {
    super(mensagem)
    this.tipo = tipo
  }
}

/** Faz a chamada e devolve o corpo da resposta. Em caso de erro, lança ErroCheckIn. */
async function chamar<T>(url: string, metodo: string, mensagemPadrao: string, corpo?: object): Promise<T> {
  const opcoes: RequestInit = {
    method: metodo,
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + obterToken(),
    },
  }
  if (corpo !== undefined) {
    opcoes.body = JSON.stringify(corpo)
  }

  let resposta: Response
  try {
    resposta = await fetch(url, opcoes)
  } catch {
    throw new ErroCheckIn('aviso', 'Não foi possível conectar ao servidor. Tente de novo.')
  }

  if (resposta.ok) {
    return resposta.json()
  }
  if (resposta.status === 401) {
    throw new ErroCheckIn('sem-login', 'Sua sessão expirou. Entre de novo.')
  }
  if (resposta.status === 403) {
    throw new ErroCheckIn('sem-permissao', 'Acesso não permitido')
  }
  // 400, 404 e 409 trazem a mensagem do back-end.
  if (resposta.status === 400 || resposta.status === 404 || resposta.status === 409) {
    const erro = await resposta.json()
    throw new ErroCheckIn('aviso', erro.mensagem)
  }
  throw new ErroCheckIn('aviso', mensagemPadrao)
}

/** Procura as senhas de hoje pelo CPF ou pelo número. */
export function buscarCheckIn(busca: string): Promise<ResultadoBusca> {
  return chamar('/api/recepcao/check-in/busca', 'POST',
    'Não foi possível fazer a busca agora. Tente de novo.', { busca: busca })
}

export function confirmarPresenca(senhaId: number): Promise<SenhaCheckIn> {
  return chamar('/api/recepcao/senhas/' + senhaId + '/check-in', 'PUT',
    'Não foi possível confirmar a presença agora. Tente de novo.')
}

export function listarServicosDaRecepcao(): Promise<ServicoDisponivel[]> {
  return chamar('/api/recepcao/servicos', 'GET', 'Não foi possível carregar os serviços. Tente de novo.')
}

export function colocarNaFila(pacienteId: number, servicoId: number): Promise<SenhaGerada> {
  return chamar('/api/recepcao/senhas', 'POST', 'Não foi possível gerar a senha agora. Tente de novo.',
    { pacienteId: pacienteId, servicoId: servicoId })
}

/** Marca a prioridade legal na senha. O motivo é obrigatório na urgência (US11, Cenário 2). */
export function marcarPrioridade(senhaId: number, prioridade: PrioridadeSenha, motivo: string): Promise<PrioridadeMarcada> {
  return chamar('/api/recepcao/senhas/' + senhaId + '/prioridade', 'PUT',
    'Não foi possível marcar a prioridade agora. Tente de novo.', { prioridade: prioridade, motivo: motivo })
}
