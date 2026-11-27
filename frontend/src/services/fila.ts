/**
 * Fila do paciente (US06 - SCRUM-137, US07 - SCRUM-140 e US08 - SCRUM-129).
 *
 * Chama o GET /api/paciente/servicos e o POST /api/paciente/senhas do back-end
 * (SCRUM-136), o GET /api/paciente/senhas (SCRUM-139) e o
 * POST /api/paciente/senhas/{id}/cancelar (SCRUM-43) com o token do paciente.
 */
import { obterToken } from './auth'

/** Serviço da tela de escolha. aberta = dentro do horário e com senhas disponíveis hoje. */
export type ServicoDisponivel = {
  id: number
  nome: string
  aberta: boolean
}

/** Senha gerada ao entrar na fila. */
export type SenhaGerada = {
  id: number
  numero: number
  servico: string
  data: string
  status: string
}

/** Senha ativa do paciente com a posição na fila (US07). posicao = pessoasAFrente + 1. */
export type PosicaoNaFila = SenhaGerada & {
  posicao: number
  pessoasAFrente: number
}

/** O que a tela deve fazer com o erro. */
export type TipoErroFila = 'nao-pode-entrar' | 'sem-login' | 'sem-permissao' | 'servidor'

/** Erro com a mensagem que deve aparecer na tela. */
export class ErroFila extends Error {
  tipo: TipoErroFila

  constructor(tipo: TipoErroFila, mensagem: string) {
    super(mensagem)
    this.tipo = tipo
  }
}

function cabecalhos(): Record<string, string> {
  return {
    'Content-Type': 'application/json',
    Authorization: 'Bearer ' + obterToken(),
  }
}

/** Trata as respostas de erro que as chamadas têm em comum. */
async function erroDaResposta(resposta: Response, mensagemPadrao: string): Promise<ErroFila> {
  if (resposta.status === 401) {
    return new ErroFila('sem-login', 'Sua sessão expirou. Entre de novo.')
  }
  if (resposta.status === 403) {
    return new ErroFila('sem-permissao', 'Acesso não permitido')
  }
  // 404 (serviço não encontrado) e 409 (fila fechada ou senha ativa) trazem a mensagem do back-end.
  if (resposta.status === 404 || resposta.status === 409 || resposta.status === 400) {
    const corpo = await resposta.json()
    return new ErroFila('nao-pode-entrar', corpo.mensagem)
  }
  return new ErroFila('servidor', mensagemPadrao)
}

/** GET que devolve uma lista. */
async function buscarLista<T>(url: string, mensagemPadrao: string): Promise<T[]> {
  let resposta: Response
  try {
    resposta = await fetch(url, { headers: cabecalhos() })
  } catch {
    throw new ErroFila('servidor', 'Não foi possível conectar ao servidor. Tente de novo.')
  }

  if (!resposta.ok) {
    throw await erroDaResposta(resposta, mensagemPadrao)
  }

  const lista = await resposta.json()
  if (!Array.isArray(lista)) {
    throw new ErroFila('servidor', mensagemPadrao)
  }
  return lista
}

export function listarServicos(): Promise<ServicoDisponivel[]> {
  return buscarLista('/api/paciente/servicos', 'Não foi possível carregar os serviços. Tente de novo.')
}

/** Senhas ativas de hoje do paciente, com a posição. Lista vazia quando ele não está em nenhuma fila. */
export function listarMinhasSenhas(): Promise<PosicaoNaFila[]> {
  return buscarLista('/api/paciente/senhas', 'Não foi possível carregar a sua posição na fila. Tente de novo.')
}

export async function entrarNaFila(servicoId: number): Promise<SenhaGerada> {
  const mensagemPadrao = 'Não foi possível entrar na fila agora. Tente de novo.'
  let resposta: Response
  try {
    resposta = await fetch('/api/paciente/senhas', {
      method: 'POST',
      headers: cabecalhos(),
      body: JSON.stringify({ servicoId: servicoId }),
    })
  } catch {
    throw new ErroFila('servidor', 'Não foi possível conectar ao servidor. Tente de novo.')
  }

  if (resposta.status === 201) {
    return resposta.json()
  }
  throw await erroDaResposta(resposta, mensagemPadrao)
}

/** Cancela a senha do paciente (US08). O back-end recusa se o atendimento já começou. */
export async function cancelarSenha(senhaId: number): Promise<SenhaGerada> {
  const mensagemPadrao = 'Não foi possível cancelar a senha agora. Tente de novo.'
  let resposta: Response
  try {
    resposta = await fetch('/api/paciente/senhas/' + senhaId + '/cancelar', {
      method: 'POST',
      headers: cabecalhos(),
    })
  } catch {
    throw new ErroFila('servidor', 'Não foi possível conectar ao servidor. Tente de novo.')
  }

  if (resposta.status === 200) {
    return resposta.json()
  }
  throw await erroDaResposta(resposta, mensagemPadrao)
}
