/**
 * Iniciar e finalizar o atendimento (US14 - SCRUM-168).
 *
 * Chama o POST /api/profissional/atendimentos/{senhaId}/inicio e /fim (SCRUM-167)
 * com o token do profissional. Os erros usam o mesmo ErroChamada da tela.
 *
 * Para a tela não perder o paciente ao recarregar, o navegador guarda só o id da senha
 * em andamento (nenhum dado do paciente) e a tela consulta o GET /{senhaId} ao abrir.
 */
import { CHAVE_SENHA_EM_ANDAMENTO, obterToken } from './auth'
import { ErroChamada } from './chamada'
import type { SenhaChamada } from './chamada'

/** Atendimento depois de iniciar ou finalizar. finalizadoEm vem nulo enquanto não termina. */
export type Atendimento = {
  id: number
  senhaId: number
  numero: number
  servico: string
  nomePaciente: string
  status: string
  iniciadoEm: string
  finalizadoEm: string | null
}

/** Senha chamada e o atendimento dela (nulo enquanto não foi iniciado). */
export type SenhaEmAndamento = {
  senha: SenhaChamada
  atendimento: Atendimento | null
}

/** Faz a requisição; só devolve a resposta se deu certo, senão lança ErroChamada. */
async function requisitar(url: string, metodo: 'GET' | 'POST', mensagemPadrao: string): Promise<Response> {
  let resposta: Response
  try {
    resposta = await fetch(url, {
      method: metodo,
      headers: { Authorization: 'Bearer ' + obterToken() },
    })
  } catch {
    throw new ErroChamada('aviso', 'Não foi possível conectar ao servidor. Tente de novo.')
  }

  if (resposta.ok) {
    return resposta
  }
  if (resposta.status === 401) {
    throw new ErroChamada('sem-login', 'Sua sessão expirou. Entre de novo.')
  }
  if (resposta.status === 403) {
    throw new ErroChamada('sem-permissao', 'Acesso não permitido')
  }
  // 404: a senha não existe ou é de outro setor. 409: por exemplo "Inicie o atendimento antes
  // de finalizar". A mensagem vem do back-end.
  if (resposta.status === 404) {
    const corpo = await resposta.json()
    throw new ErroChamada('nao-encontrada', corpo.mensagem)
  }
  if (resposta.status === 409) {
    const corpo = await resposta.json()
    throw new ErroChamada('aviso', corpo.mensagem)
  }
  throw new ErroChamada('aviso', mensagemPadrao)
}

async function enviar(senhaId: number, acao: 'inicio' | 'fim', mensagemPadrao: string): Promise<Atendimento> {
  const resposta = await requisitar(`/api/profissional/atendimentos/${senhaId}/${acao}`, 'POST', mensagemPadrao)
  return resposta.json()
}

/** Situação atual da senha e do atendimento, para recuperar a tela depois de recarregar. */
export async function consultarSenhaEmAndamento(senhaId: number): Promise<SenhaEmAndamento> {
  const resposta = await requisitar(
    `/api/profissional/atendimentos/${senhaId}`,
    'GET',
    'Não foi possível recuperar o paciente chamado. Tente de novo.',
  )
  return resposta.json()
}

export function guardarSenhaEmAndamento(senhaId: number): void {
  localStorage.setItem(CHAVE_SENHA_EM_ANDAMENTO, String(senhaId))
}

/** Id da senha chamada que ainda não foi finalizada, ou null. */
export function obterSenhaEmAndamento(): number | null {
  const valor = Number(localStorage.getItem(CHAVE_SENHA_EM_ANDAMENTO))
  if (Number.isInteger(valor) && valor > 0) {
    return valor
  }
  return null
}

export function esquecerSenhaEmAndamento(): void {
  localStorage.removeItem(CHAVE_SENHA_EM_ANDAMENTO)
}

export function iniciarAtendimento(senhaId: number): Promise<Atendimento> {
  return enviar(senhaId, 'inicio', 'Não foi possível iniciar o atendimento agora. Tente de novo.')
}

export function finalizarAtendimento(senhaId: number): Promise<Atendimento> {
  return enviar(senhaId, 'fim', 'Não foi possível finalizar o atendimento agora. Tente de novo.')
}
