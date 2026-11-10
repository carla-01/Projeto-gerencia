import { obterToken } from './auth'

export type DadosConfiguracaoFila = {
  servico: string
  horaInicio: string
  horaFim: string
  limiteSenhas: number
}

const URL_CONFIGURACAO = '/api/gestor/servicos'

/** Cria o serviço com horário de funcionamento e limite diário, usando a sessão atual do gestor. */
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
      // Nomes do ServicoRequest do back-end (SCRUM-131 e SCRUM-132).
      body: JSON.stringify({
        nome: dados.servico,
        horarioAbertura: dados.horaInicio,
        horarioFechamento: dados.horaFim,
        limiteSenhasDia: dados.limiteSenhas,
      }),
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
  if (resposta.status === 400 || resposta.status === 409) {
    // O back-end explica o erro, por exemplo "Horário inválido" ou "Serviço já cadastrado".
    const mensagem = await lerMensagem(resposta)
    if (mensagem !== null) {
      throw new Error(mensagem)
    }
  }
  throw new Error('Não foi possível salvar a configuração agora. Tente de novo.')
}

async function lerMensagem(resposta: Response): Promise<string | null> {
  try {
    const corpo = await resposta.json()
    return typeof corpo?.mensagem === 'string' ? corpo.mensagem : null
  } catch {
    return null
  }
}