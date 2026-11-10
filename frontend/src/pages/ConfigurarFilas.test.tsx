import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import ConfigurarFilas from './ConfigurarFilas'
import { salvarConfiguracaoFila } from '../services/filas'

vi.mock('../services/filas', () => ({
  salvarConfiguracaoFila: vi.fn(),
}))

const salvarConfiguracaoFilaMock = vi.mocked(salvarConfiguracaoFila)

function preencherFormulario(horaFim: string) {
  fireEvent.change(screen.getByLabelText('Serviço'), { target: { value: 'Clínico' } })
  fireEvent.change(screen.getByLabelText('Horário de início'), { target: { value: '07:00' } })
  fireEvent.change(screen.getByLabelText('Horário de fim'), { target: { value: horaFim } })
  fireEvent.change(screen.getByLabelText('Limite de senhas por dia'), { target: { value: '40' } })
}

describe('ConfigurarFilas', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('CT-US09-01: salva a configuração do serviço Clínico', async () => {
    salvarConfiguracaoFilaMock.mockResolvedValue(undefined)
    render(
      <MemoryRouter>
        <ConfigurarFilas />
      </MemoryRouter>,
    )

    preencherFormulario('12:00')
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() => {
      expect(salvarConfiguracaoFilaMock).toHaveBeenCalledWith({
        servico: 'Clínico',
        horaInicio: '07:00',
        horaFim: '12:00',
        limiteSenhas: 40,
      })
    })
    expect(await screen.findByRole('status')).toHaveTextContent('Configuração salva com sucesso.')
  })

  it('CT-US09-02: rejeita horário de fim anterior ao início sem chamar a API', () => {
    render(
      <MemoryRouter>
        <ConfigurarFilas />
      </MemoryRouter>,
    )

    preencherFormulario('06:00')
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Horário inválido')
    expect(salvarConfiguracaoFilaMock).not.toHaveBeenCalled()
  })

  it('restringe a rota ao perfil gestor', () => {
    localStorage.setItem('filasus.token', 'token-de-teste')
    localStorage.setItem('filasus.perfil', 'PACIENTE')
    render(
      <MemoryRouter initialEntries={['/gestor/configurar-filas']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Acesso não permitido' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Configurar filas e horários' })).not.toBeInTheDocument()
  })

  it('permite que o gestor abra a rota de configuração', () => {
    localStorage.setItem('filasus.token', 'token-de-teste')
    localStorage.setItem('filasus.perfil', 'GESTOR')
    render(
      <MemoryRouter initialEntries={['/gestor/configurar-filas']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Configurar filas e horários' })).toBeInTheDocument()
  })
})
