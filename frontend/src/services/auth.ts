/**
 * Login do FilaSUS (US01 - SCRUM-37).
 *
 * Chama o POST /auth/login do back-end e guarda o token no navegador.
 */

export type Perfil = 'PACIENTE' | 'RECEPCIONISTA' | 'PROFISSIONAL' | 'GESTOR'

type RespostaLogin = {
  token: string
  perfil: Perfil
}

const CHAVE_TOKEN = 'filasus.token'
const CHAVE_PERFIL = 'filasus.perfil'

/** Erro com a mensagem que deve aparecer na tela. */
export class ErroLogin extends Error {}

/**
 * Envia CPF (ou e-mail) e senha. Se der certo, guarda o token e devolve o perfil.
 * Se der errado, lança ErroLogin com a mensagem para mostrar ao usuário.
 */
export async function fazerLogin(login: string, senha: string): Promise<Perfil> {
  let resposta: Response
  try {
    resposta = await fetch('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ login: login, senha: senha }),
    })
  } catch {
    throw new ErroLogin('Não foi possível conectar ao servidor. Tente de novo.')
  }

  if (resposta.status === 401) {
    // O back-end manda sempre "CPF ou senha inválidos" (não revela se o CPF existe).
    const corpo = await resposta.json()
    throw new ErroLogin(corpo.mensagem)
  }

  if (resposta.status === 400) {
    throw new ErroLogin('Preencha o CPF ou e-mail e a senha.')
  }

  if (!resposta.ok) {
    throw new ErroLogin('Não foi possível entrar agora. Tente de novo.')
  }

  const dados: RespostaLogin = await resposta.json()
  localStorage.setItem(CHAVE_TOKEN, dados.token)
  localStorage.setItem(CHAVE_PERFIL, dados.perfil)
  return dados.perfil
}

/** Endereço da tela inicial de cada perfil. */
export function telaInicialDoPerfil(perfil: Perfil): string {
  if (perfil === 'PACIENTE') {
    return '/paciente'
  }
  if (perfil === 'RECEPCIONISTA') {
    return '/recepcao'
  }
  if (perfil === 'PROFISSIONAL') {
    return '/profissional'
  }
  return '/gestor'
}
