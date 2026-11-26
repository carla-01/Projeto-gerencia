import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'

/** Chamar o próximo (US13 - SCRUM-164). O back-end é simulado e os dados são fictícios. */

const SENHA = {
  id: 41,
  numero: 7,
  servico: 'Clínico',
  nomePaciente: 'Paciente Fictício',
  prioridade: 'IDOSO',
  status: 'CHAMADA',
  chamadaEm: '2026-11-25T12:00:00Z',
}
const CHAMOU = { mensagem: null, senha: SENHA }
const FILA_VAZIA = { mensagem: 'Não há pacientes aguardando', senha: null }

type RespostaSimulada = { status: number; corpo: unknown }

/** Respostas do POST /api/profissional/chamadas, na ordem; a última se repete. */
function simularBackEnd(...respostas: RespostaSimulada[]) {
  let quantidade = 0
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async () => {
    const resposta = respostas[Math.min(quantidade, respostas.length - 1)]
    quantidade++
    return new Response(JSON.stringify(resposta.corpo), {
      status: resposta.status,
      headers: { 'Content-Type': 'application/json' },
    })
  })
}

function abrirComo(perfil: string) {
  localStorage.setItem('filasus.token', 'token-de-teste')
  localStorage.setItem('filasus.perfil', perfil)
  render(
    <MemoryRouter initialEntries={['/profissional']}>
      <App />
    </MemoryRouter>,
  )
}

function chamarProximo() {
  fireEvent.click(screen.getByRole('button', { name: 'Chamar próximo' }))
}

describe('Tela do profissional', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('CT-US13-01: chama o próximo e mostra a senha, o paciente e a prioridade', async () => {
    const fetch = simularBackEnd({ status: 200, corpo: CHAMOU })
    abrirComo('PROFISSIONAL')

    chamarProximo()

    expect(await screen.findByText('007')).toBeInTheDocument()
    expect(screen.getByText('Paciente Fictício')).toBeInTheDocument()
    expect(screen.getByText('Idoso 60+')).toBeInTheDocument()
    const [url, opcoes] = fetch.mock.calls[0]
    expect(url).toBe('/api/profissional/chamadas')
    expect(opcoes?.method).toBe('POST')
  })

  it('CT-US13-02: com a fila vazia mostra "Não há pacientes aguardando"', async () => {
    simularBackEnd({ status: 200, corpo: FILA_VAZIA })
    abrirComo('PROFISSIONAL')

    chamarProximo()

    expect(await screen.findByText('Não há pacientes aguardando')).toBeInTheDocument()
    expect(screen.getByText('Nenhum paciente chamado ainda.')).toBeInTheDocument()
  })

  it('mantém o último chamado na tela quando a fila acaba', async () => {
    simularBackEnd({ status: 200, corpo: CHAMOU }, { status: 200, corpo: FILA_VAZIA })
    abrirComo('PROFISSIONAL')

    chamarProximo()
    await screen.findByText('007')
    fireEvent.click(await screen.findByRole('button', { name: 'Chamar próximo' }))

    expect(await screen.findByText('Não há pacientes aguardando')).toBeInTheDocument()
    expect(screen.getByText('007')).toBeInTheDocument()
  })

  it('avisa quando o profissional não tem setor', async () => {
    simularBackEnd({
      status: 409,
      corpo: { mensagem: 'Seu usuário não tem setor. Peça ao gestor para configurar' },
    })
    abrirComo('PROFISSIONAL')

    chamarProximo()

    expect(await screen.findByRole('alert')).toHaveTextContent('Seu usuário não tem setor')
  })

  it('outro perfil não abre a tela do profissional', () => {
    abrirComo('PACIENTE')

    expect(screen.getByRole('heading', { name: 'Acesso não permitido' })).toBeInTheDocument()
  })
})
