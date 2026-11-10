import { obterToken } from './auth'

export type DadosConfiguracaoFila = {
  servico: string
  horaInicio: string
  horaFim: string
  limiteSenhas: number
}

const URL_CONFIGURACAO = '/api/gestor/servicos'

/**
 * Cria configuração de serviço usando a sessão atual do gestor.
 * TODO: alinhar os nomes e o limite diário com o DTO do back-end; o endpoint atual
 * ainda declara nome/horarioAbertura/horarioFechamento e não recebe limiteSenhas.
 */
export async function salvarConfiguracaoFila(dados: DadosConfiguracaoFila): Promise<void> {
  const token = obterToken()
  if (token === null) {
    throw new Error('Sua sessão expirou. Entre de novo.')
  }

  let resposta: Response
  try {
    resposta = await fetch(URL_CONFIGURACAO, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(dados),
    })
  } catch {
    throw new Error('Não foi possível conectar ao servidor. Tente de novo.')
  }

  if (resposta.ok) {
    return
  }
  if (resposta.status === 401) {
    throw new Error('Sua sessão expirou. Entre de novo.')
  }
  if (resposta.status === 403) {
    throw new Error('Acesso não permitido')
  }
  throw new Error('Não foi possível salvar a configuração agora. Tente de novo.')
}
