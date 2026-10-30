import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'

/**
 * Cenários Gherkin da US01 - Login por perfil (SCRUM-37), pela tela.
 * O back-end é simulado: o fetch devolve a mesma resposta que o POST /auth/login devolveria.
 */

/**
 * Faz o fetch responder com o status e o corpo informados.
 * Cria uma resposta nova a cada chamada, porque o corpo de uma resposta só pode ser lido uma vez.
 */
function simularResposta(status: number, corpo: object) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async () => {
    return new Response(JSON.stringify(corpo), {
      status: status,
      headers: { 'Content-Type': 'application/json' },
    })
  })
}

/** Abre a tela de login, preenche os campos e clica em Entrar. */
function entrarCom(login: string, senha: string) {
  fireEvent.change(screen.getByLabelText('CPF ou e-mail'), { target: { value: login } })
  fireEvent.change(screen.getByLabelText('Senha'), { target: { value: senha } })
  fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))
}

function abrirTelaDeLogin() {
  render(
    <MemoryRouter initialEntries={['/login']}>
      <App />
    </MemoryRouter>,
  )
}

describe('US01 - Login por perfil', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // CT-US01-01 - Login com sucesso
  // Dado que estou cadastrado e ativo
  // Quando informo CPF e senha corretos
  // Então sou levado à tela inicial do meu perfil
  describe('CT-US01-01 - Login com sucesso', () => {
    it('envia CPF e senha, guarda o token e abre a tela inicial do perfil', async () => {
      const fetch = simularResposta(200, { token: 'token-de-teste', perfil: 'GESTOR' })
      abrirTelaDeLogin()

      entrarCom('123.456.789-01', 'senha123')

      expect(await screen.findByRole('heading', { name: 'Gestor' })).toBeInTheDocument()
      expect(localStorage.getItem('filasus.token')).toBe('token-de-teste')
      expect(localStorage.getItem('filasus.perfil')).toBe('GESTOR')

      // Confere o que foi enviado para o back-end.
      expect(fetch).toHaveBeenCalledTimes(1)
      const [url, opcoes] = fetch.mock.calls[0]
      expect(url).toBe('/auth/login')
      expect(opcoes?.method).toBe('POST')
      expect(JSON.parse(String(opcoes?.body))).toEqual({ login: '123.456.789-01', senha: 'senha123' })
    })

    // Cada perfil vai para a sua própria tela inicial.
    const perfis = [
      { perfil: 'PACIENTE', titulo: 'Área do paciente' },
      { perfil: 'RECEPCIONISTA', titulo: 'Painel da recepção' },
      { perfil: 'PROFISSIONAL', titulo: 'Profissional de saúde' },
      { perfil: 'GESTOR', titulo: 'Gestor' },
    ]

    it.each(perfis)('perfil $perfil abre a tela "$titulo"', async ({ perfil, titulo }) => {
      // O login devolve o token; o painel da recepção (US02) busca a fila do dia logo depois.
      vi.spyOn(globalThis, 'fetch').mockImplementation(async (url) => {
        let corpo: object = { token: 'token-de-teste', perfil: perfil }
        if (url === '/api/recepcao/fila') {
          corpo = { usuarioId: '7', fila: [] }
        }
        return new Response(JSON.stringify(corpo), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      })
      abrirTelaDeLogin()

      entrarCom('12345678901', 'senha123')

      expect(await screen.findByRole('heading', { name: titulo })).toBeInTheDocument()
    })
  })

  // CT-US01-02 - Senha incorreta
  // Dado que estou cadastrado
  // Quando informo a senha errada 5 vezes seguidas
  // Então vejo "CPF ou senha inválidos" e a conta fica bloqueada por 15 minutos
  // (o bloqueio de 15 minutos é testado no back-end, em LoginBloqueioTest)
  describe('CT-US01-02 - Senha incorreta', () => {
    it('mostra "CPF ou senha inválidos" a cada uma das 5 tentativas e não entra', async () => {
      simularResposta(401, { mensagem: 'CPF ou senha inválidos' })
      abrirTelaDeLogin()

      for (let tentativa = 1; tentativa <= 5; tentativa++) {
        entrarCom('12345678901', 'senhaErrada')

        expect(await screen.findByRole('alert')).toHaveTextContent('CPF ou senha inválidos')
        // O botão volta a ficar disponível para a próxima tentativa.
        expect(await screen.findByRole('button', { name: 'Entrar' })).toBeEnabled()
      }

      // Continua na tela de login e nada foi guardado.
      expect(screen.getByRole('heading', { name: 'Bem-vindo ao FilaSUS' })).toBeInTheDocument()
      expect(localStorage.getItem('filasus.token')).toBeNull()
    })
  })
})
