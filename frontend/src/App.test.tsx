import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { fazerLogout } from './services/auth'

/** Teste do esqueleto: garante que as rotas sobem. Cada história acrescenta os seus. */
describe('App', () => {
  beforeEach(() => {
    fazerLogout()
    vi.restoreAllMocks()
  })

  it('redireciona a raiz para a tela de login', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    )
    expect(screen.getByRole('heading', { name: 'Bem-vindo ao FilaSUS' })).toBeInTheDocument()
  })

  it('mostra aviso em rota inexistente', () => {
    render(
      <MemoryRouter initialEntries={['/nao-existe']}>
        <App />
      </MemoryRouter>,
    )
    expect(screen.getByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument()
  })

  it('CT-US02-01: recepcionista logado acessa o painel da recepção e vê a fila do dia', async () => {
    localStorage.setItem('filasus.token', 'token-recepcionista')
    localStorage.setItem('filasus.perfil', 'RECEPCIONISTA')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ usuarioId: '7', fila: [] }),
    }))

    render(
      <MemoryRouter initialEntries={['/recepcao']}>
        <App />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: 'Painel da recepção' })).toBeInTheDocument()
    expect(screen.getByText('Nenhuma pessoa na fila.')).toBeInTheDocument()
  })

  it('CT-US02-02: paciente logado na recepção vê acesso não permitido', () => {
    localStorage.setItem('filasus.token', 'token-paciente')
    localStorage.setItem('filasus.perfil', 'PACIENTE')

    render(
      <MemoryRouter initialEntries={['/recepcao']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Acesso não permitido' })).toBeInTheDocument()
  })

  it('usuário sem login em /recepcao é encaminhado para /login', () => {
    render(
      <MemoryRouter initialEntries={['/recepcao']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Bem-vindo ao FilaSUS' })).toBeInTheDocument()
  })
})
