import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import App from './App'

/** Teste do esqueleto: garante que as rotas sobem. Cada história acrescenta os seus. */
describe('App', () => {
  it('redireciona a raiz para a tela de login', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    )
    expect(screen.getByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
  })

  it('mostra aviso em rota inexistente', () => {
    render(
      <MemoryRouter initialEntries={['/nao-existe']}>
        <App />
      </MemoryRouter>,
    )
    expect(screen.getByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument()
  })
})
