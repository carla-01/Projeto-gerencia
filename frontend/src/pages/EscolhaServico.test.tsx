import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'

/**
 * Escolha de serviço e entrada na fila (US06 - SCRUM-137), pela tela.
 * O back-end é simulado: o fetch devolve o que o GET /api/paciente/servicos e o
 * POST /api/paciente/senhas devolveriam. Só com dados fictícios.
 */

const SERVICOS = [
  { id: 1, nome: 'Clínico', aberta: true },
  { id: 2, nome: 'Odontologia', aberta: false },
]

type RespostaSimulada = { status: number; corpo: unknown }

/** Responde a lista de serviços e, no POST, a resposta informada. */
function simularBackEnd(entrada: RespostaSimulada = { status: 201, corpo: {} }, servicos: unknown = SERVICOS) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (_url, opcoes) => {
    let resposta: RespostaSimulada = { status: 200, corpo: servicos }
    if (opcoes?.method === 'POST') {
      resposta = entrada
    }
    return new Response(JSON.stringify(resposta.corpo), {
      status: resposta.status,
      headers: { 'Content-Type': 'application/json' },
    })
  })
}

function abrirComoPaciente() {
  localStorage.setItem('filasus.token', 'token-paciente')
  localStorage.setItem('filasus.perfil', 'PACIENTE')
  render(
    <MemoryRouter initialEntries={['/paciente']}>
      <App />
    </MemoryRouter>,
  )
}

describe('US06 - Entrar na fila', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('mostra os serviços de hoje dizendo se a fila está aberta', async () => {
    const fetch = simularBackEnd()
    abrirComoPaciente()

    expect(screen.getByRole('heading', { name: 'Área do paciente' })).toBeInTheDocument()
    expect(await screen.findByText('Clínico')).toBeInTheDocument()
    expect(screen.getByText('Odontologia')).toBeInTheDocument()
    expect(screen.getByText('Fila aberta')).toBeInTheDocument()
    expect(screen.getByText('Fila fechada')).toBeInTheDocument()

    const [url, opcoes] = fetch.mock.calls[0]
    expect(url).toBe('/api/paciente/servicos')
    const cabecalhos = opcoes?.headers as Record<string, string>
    expect(cabecalhos.Authorization).toBe('Bearer token-paciente')
  })

  it('não deixa entrar na fila de um serviço fechado', async () => {
    simularBackEnd()
    abrirComoPaciente()

    expect(await screen.findByRole('button', { name: 'Entrar na fila de Odontologia' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Entrar na fila de Clínico' })).toBeEnabled()
  })

  it('entra na fila e mostra o número da senha', async () => {
    const fetch = simularBackEnd({
      status: 201,
      corpo: { id: 9, numero: 6, servico: 'Clínico', data: '2026-11-10', status: 'AGUARDANDO' },
    })
    abrirComoPaciente()

    fireEvent.click(await screen.findByRole('button', { name: 'Entrar na fila de Clínico' }))

    expect(await screen.findByRole('heading', { name: 'Você entrou na fila' })).toBeInTheDocument()
    expect(screen.getByText('6')).toBeInTheDocument()
    expect(screen.getByText(/Senha de 10\/11\/2026/)).toBeInTheDocument()

    // Confere o que foi enviado: o serviço escolhido, com o token do paciente.
    const [url, opcoes] = fetch.mock.calls[1]
    expect(url).toBe('/api/paciente/senhas')
    expect(opcoes?.method).toBe('POST')
    expect(JSON.parse(String(opcoes?.body))).toEqual({ servicoId: 1 })
  })

  it('mostra "Fila fechada" quando a fila fecha antes de o paciente entrar', async () => {
    simularBackEnd({ status: 409, corpo: { mensagem: 'Fila fechada' } })
    abrirComoPaciente()

    fireEvent.click(await screen.findByRole('button', { name: 'Entrar na fila de Clínico' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Fila fechada')
    expect(screen.queryByRole('heading', { name: 'Você entrou na fila' })).not.toBeInTheDocument()
  })

  it('avisa quando o paciente já tem uma senha ativa no serviço', async () => {
    simularBackEnd({ status: 409, corpo: { mensagem: 'Você já tem uma senha ativa neste serviço' } })
    abrirComoPaciente()

    fireEvent.click(await screen.findByRole('button', { name: 'Entrar na fila de Clínico' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Você já tem uma senha ativa neste serviço')
  })

  it('avisa quando não há serviços disponíveis', async () => {
    simularBackEnd(undefined, [])
    abrirComoPaciente()

    expect(await screen.findByText('Nenhum serviço disponível hoje.')).toBeInTheDocument()
  })

  it('volta para o login quando a sessão expira', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{}', { status: 401 }))
    abrirComoPaciente()

    expect(await screen.findByRole('heading', { name: 'Bem-vindo ao FilaSUS' })).toBeInTheDocument()
    expect(localStorage.getItem('filasus.token')).toBeNull()
  })

  it('outro perfil não abre a área do paciente', () => {
    localStorage.setItem('filasus.token', 'token-recepcionista')
    localStorage.setItem('filasus.perfil', 'RECEPCIONISTA')
    render(
      <MemoryRouter initialEntries={['/paciente']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Acesso não permitido' })).toBeInTheDocument()
  })
})
