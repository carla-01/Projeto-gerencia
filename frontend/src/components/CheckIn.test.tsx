import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'

/**
 * Check-in na recepção (US10 - SCRUM-156). O back-end é simulado e os dados são fictícios.
 */

const SENHA = {
  id: 31,
  numero: 7,
  servico: 'Clínico',
  nomePaciente: 'Paciente Fictício',
  cpfMascarado: '529.***.***-25',
  status: 'AGUARDANDO',
  prioridade: 'IDOSO',
  checkinEm: null,
}

const COM_SENHA = { mensagem: null, pacienteId: 5, nomePaciente: 'Paciente Fictício', senhas: [SENHA] }
const SEM_SENHA = {
  mensagem: 'Paciente sem senha hoje',
  pacienteId: 5,
  nomePaciente: 'Paciente Fictício',
  senhas: [],
}

type RespostaSimulada = { status: number; corpo: unknown }

type BackEndSimulado = {
  /** Respostas da busca, na ordem; a última se repete. */
  buscas?: RespostaSimulada[]
  confirmacao?: RespostaSimulada
  geracao?: RespostaSimulada
}

function simularBackEnd({
  buscas = [{ status: 200, corpo: COM_SENHA }],
  confirmacao = { status: 200, corpo: { ...SENHA, status: 'PRESENTE', checkinEm: '2026-11-24T12:00:00Z' } },
  geracao = { status: 201, corpo: { id: 31, numero: 7, servico: 'Clínico' } },
}: BackEndSimulado = {}) {
  let quantidadeDeBuscas = 0
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (url) => {
    let resposta: RespostaSimulada = { status: 200, corpo: { usuarioId: '7', fila: [] } }
    if (url === '/api/recepcao/check-in/busca') {
      resposta = buscas[Math.min(quantidadeDeBuscas, buscas.length - 1)]
      quantidadeDeBuscas++
    } else if (url === '/api/recepcao/senhas/31/check-in') {
      resposta = confirmacao
    } else if (url === '/api/recepcao/servicos') {
      resposta = {
        status: 200,
        corpo: [
          { id: 1, nome: 'Clínico', aberta: true },
          { id: 2, nome: 'Odontologia', aberta: false },
        ],
      }
    } else if (url === '/api/recepcao/senhas') {
      resposta = geracao
    }
    return new Response(JSON.stringify(resposta.corpo), {
      status: resposta.status,
      headers: { 'Content-Type': 'application/json' },
    })
  })
}

async function buscar(texto: string) {
  localStorage.setItem('filasus.token', 'token-recepcionista')
  localStorage.setItem('filasus.perfil', 'RECEPCIONISTA')
  render(
    <MemoryRouter initialEntries={['/recepcao']}>
      <App />
    </MemoryRouter>,
  )
  fireEvent.change(await screen.findByLabelText('CPF ou número da senha'), { target: { value: texto } })
  fireEvent.click(screen.getByRole('button', { name: 'Buscar' }))
}

describe('Check-in na recepção', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('CT-US10-01: busca pelo CPF, confirma a presença e mostra "Presente"', async () => {
    const fetch = simularBackEnd()

    await buscar('52998224725')
    expect(await screen.findByText('007')).toBeInTheDocument()
    expect(screen.getByText('Idoso 60+')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar presença' }))

    expect(await screen.findByText('Presente')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Confirmar presença' })).not.toBeInTheDocument()

    // O CPF vai com a máscara no corpo da busca, e não no endereço.
    const chamada = fetch.mock.calls.find(([url]) => url === '/api/recepcao/check-in/busca')
    expect(JSON.parse(String(chamada?.[1]?.body))).toEqual({ busca: '529.982.247-25' })
  })

  it('CT-US10-02: sem senha hoje, oferece colocar na fila e gera a senha', async () => {
    const fetch = simularBackEnd({
      buscas: [
        { status: 200, corpo: SEM_SENHA },
        { status: 200, corpo: COM_SENHA },
      ],
    })

    await buscar('52998224725')
    expect(await screen.findByText('Paciente sem senha hoje')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Colocar na fila' }))

    // A fila fechada não pode ser escolhida.
    expect(await screen.findByRole('radio', { name: /Odontologia/ })).toBeDisabled()
    fireEvent.click(screen.getByRole('radio', { name: /Clínico/ }))
    fireEvent.click(screen.getByRole('button', { name: 'Gerar senha' }))

    expect(await screen.findByText('Senha 007 gerada para Clínico.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar presença' })).toBeInTheDocument()
    const chamada = fetch.mock.calls.find(([url]) => url === '/api/recepcao/senhas')
    expect(JSON.parse(String(chamada?.[1]?.body))).toEqual({ pacienteId: 5, servicoId: 1 })
  })

  it('pede para escolher o serviço antes de gerar a senha', async () => {
    simularBackEnd({ buscas: [{ status: 200, corpo: SEM_SENHA }] })

    await buscar('52998224725')
    fireEvent.click(await screen.findByRole('button', { name: 'Colocar na fila' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Gerar senha' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Escolha o serviço')
  })

  it('CPF sem cadastro mostra o atalho para cadastrar o paciente', async () => {
    simularBackEnd({
      buscas: [{ status: 200, corpo: { ...SEM_SENHA, pacienteId: null, nomePaciente: null } }],
    })

    await buscar('39053344705')

    expect(await screen.findByRole('link', { name: 'Cadastrar paciente', description: '' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Colocar na fila' })).not.toBeInTheDocument()
  })

  it('mostra a mensagem do back-end quando o CPF é inválido', async () => {
    simularBackEnd({ buscas: [{ status: 400, corpo: { mensagem: 'CPF inválido' } }] })

    await buscar('12345678900')

    expect(await screen.findByRole('alert')).toHaveTextContent('CPF inválido')
  })

  it('não chama o back-end com a busca vazia', async () => {
    const fetch = simularBackEnd()

    await buscar('')

    expect(screen.getByRole('alert')).toHaveTextContent('Informe o CPF ou o número da senha')
    expect(fetch.mock.calls.find(([url]) => url === '/api/recepcao/check-in/busca')).toBeUndefined()
  })
})
