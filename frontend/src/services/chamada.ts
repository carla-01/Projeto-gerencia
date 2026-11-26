/**
 * "Chamar próximo" do profissional de saúde (US13 - SCRUM-164).
 *
 * Chama o POST /api/profissional/chamadas (SCRUM-162) com o token do profissional.
 */
import { obterToken } from './auth'
import type { PrioridadeSenha } from './checkIn'

/** Senha que acabou de ser chamada. */
export type SenhaChamada = {
  id: number
  numero: number
  servico: string
  nomePaciente: string
  prioridade: PrioridadeSenha
  status: string
  chamadaEm: string
}

/** Com a fila vazia, senha vem nula e mensagem explica. */
export type ResultadoChamada = {
  mensagem: string | null
  senha: SenhaChamada | null
}

/** O que a tela deve fazer com o erro. 'nao-encontrada': a senha não existe ou é de outro setor (US14). */
export type TipoErroChamada = 'aviso' | 'sem-login' | 'sem-permissao' | 'nao-encontrada'

/** Erro com a mensagem que deve aparecer na tela. */
export class ErroChamada extends Error {
  tipo: TipoErroChamada

  constructor(tipo: TipoErroChamada, mensagem: string) {
    super(mensagem)
    this.tipo = tipo
  }
}

export async function chamarProximo(): Promise<ResultadoChamada> {
  const mensagemPadrao = 'Não foi possível chamar o próximo agora. Tente de novo.'
  let resposta: Response
  try {
    resposta = await fetch('/api/profissional/chamadas', {
      method: 'POST',
      headers: { Authorization: 'Bearer ' + obterToken() },
    })
  } catch {
    throw new ErroChamada('aviso', 'Não foi possível conectar ao servidor. Tente de novo.')
  }

  if (resposta.ok) {
    return resposta.json()
  }
  if (resposta.status === 401) {
    throw new ErroChamada('sem-login', 'Sua sessão expirou. Entre de novo.')
  }
  if (resposta.status === 403) {
    throw new ErroChamada('sem-permissao', 'Acesso não permitido')
  }
  // 409: profissional sem setor. A mensagem vem do back-end.
  if (resposta.status === 409) {
    const corpo = await resposta.json()
    throw new ErroChamada('aviso', corpo.mensagem)
  }
  throw new ErroChamada('aviso', mensagemPadrao)
}
