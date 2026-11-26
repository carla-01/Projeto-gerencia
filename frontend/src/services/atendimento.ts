/**
 * Iniciar e finalizar o atendimento (US14 - SCRUM-168).
 *
 * Chama o POST /api/profissional/atendimentos/{senhaId}/inicio e /fim (SCRUM-167)
 * com o token do profissional. Os erros usam o mesmo ErroChamada da tela.
 */
import { obterToken } from './auth'
import { ErroChamada } from './chamada'

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

async function enviar(senhaId: number, acao: 'inicio' | 'fim', mensagemPadrao: string): Promise<Atendimento> {
  let resposta: Response
  try {
    resposta = await fetch(`/api/profissional/atendimentos/${senhaId}/${acao}`, {
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
  // 404: senha de outro setor. 409: por exemplo "Inicie o atendimento antes de finalizar".
  // A mensagem vem do back-end.
  if (resposta.status === 404 || resposta.status === 409) {
    const corpo = await resposta.json()
    throw new ErroChamada('aviso', corpo.mensagem)
  }
  throw new ErroChamada('aviso', mensagemPadrao)
}

export function iniciarAtendimento(senhaId: number): Promise<Atendimento> {
  return enviar(senhaId, 'inicio', 'Não foi possível iniciar o atendimento agora. Tente de novo.')
}

export function finalizarAtendimento(senhaId: number): Promise<Atendimento> {
  return enviar(senhaId, 'fim', 'Não foi possível finalizar o atendimento agora. Tente de novo.')
}
