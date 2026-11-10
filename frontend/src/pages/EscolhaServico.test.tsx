import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'

/**
 * Área do paciente: entrada na fila (US06 - SCRUM-137) e posição na fila (US07 - SCRUM-140).
 * O back-end é simulado: o fetch devolve o que o GET /api/paciente/servicos, o
 * GET /api/paciente/senhas e o POST /api/paciente/senhas devolveriam. Só com dados fictícios.
 */

const SERVICOS = [
  { id: 1, nome: 'Clínico', aberta: true },
  { id: 2, nome: 'Odontologia', aberta: false },
]

const SENHA_CLINICO = {
  id: 9,
  numero: 6,
  servico: 'Clínico',
  data: '2026-11-10',
  status: 'AGUARDANDO',
  posicao: 3,
  pessoasAFrente: 2,
}

type RespostaSimulada = { status: number; corpo: unknown }

type BackEndSimulado = {
  servicos?: unknown
  /** Respostas do GET /api/paciente/senhas, na ordem; a última se repete. */
  senhas?: RespostaSimulada[]
  entrada?: RespostaSimulada
}

function simularBackEnd({
  servicos = SERVICOS,
  senhas = [{ status: 200, corpo: [] }],
  entrada = { status: 201, corpo: {} },
}: BackEndSimulado = {}) {
  let consultasDeSenha = 0
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (url, opcoes) => {
    let resposta: RespostaSimulada = { status: 200, corpo: servicos }
    if (url === '/api/paciente/senhas' && opcoes?.method === 'POST') {
      resposta = entrada
    } else if (url === '/api/paciente/senhas') {
      resposta = senhas[Math.min(consultasDeSenha, senhas.length - 1)]
      consultasDeSenha++
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

describe('Área do paciente', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('US06 - Entrar na fila', () => {
    it('mostra os serviços de hoje dizendo se a fila está aberta', async () => {
      const fetch = simularBackEnd()
      abrirComoPaciente()

      expect(screen.getByRole('heading', { name: 'Área do paciente' })).toBeInTheDocument()
      expect(await screen.findByText('Clínico')).toBeInTheDocument()
      expect(screen.getByText('Odontologia')).toBeInTheDocument()
      expect(screen.getByText('Fila aberta')).toBeInTheDocument()
      expect(screen.getByText('Fila fechada')).toBeInTheDocument()

      const chamada = fetch.mock.calls.find(([url]) => url === '/api/paciente/servicos')
      const cabecalhos = chamada?.[1]?.headers as Record<string, string>
      expect(cabecalhos.Authorization).toBe('Bearer token-paciente')
    })

    it('não deixa entrar na fila de um serviço fechado', async () => {
      simularBackEnd()
      abrirComoPaciente()

      expect(await screen.findByRole('button', { name: 'Entrar na fila de Odontologia' })).toBeDisabled()
      expect(screen.getByRole('button', { name: 'Entrar na fila de Clínico' })).toBeEnabled()
    })

    it('entra na fila e mostra a senha com a posição', async () => {
      const fetch = simularBackEnd({
        senhas: [
          { status: 200, corpo: [] },
          { status: 200, corpo: [SENHA_CLINICO] },
        ],
        entrada: { status: 201, corpo: { id: 9, numero: 6, servico: 'Clínico', data: '2026-11-10', status: 'AGUARDANDO' } },
      })
      abrirComoPaciente()

      fireEvent.click(await screen.findByRole('button', { name: 'Entrar na fila de Clínico' }))

      expect(await screen.findByRole('status')).toHaveTextContent('Você entrou na fila de Clínico.')
      expect(await screen.findByRole('heading', { name: 'Sua senha' })).toBeInTheDocument()
      expect(screen.getByText('6')).toBeInTheDocument()
      expect(screen.getByText('3º')).toBeInTheDocument()
      expect(screen.getByText(/Senha de 10\/11\/2026/)).toBeInTheDocument()

      // Confere o que foi enviado: o serviço escolhido, com o token do paciente.
      const envio = fetch.mock.calls.find(([, opcoes]) => opcoes?.method === 'POST')
      expect(envio?.[0]).toBe('/api/paciente/senhas')
      expect(JSON.parse(String(envio?.[1]?.body))).toEqual({ servicoId: 1 })
    })

    it('mostra "Fila fechada" quando a fila fecha antes de o paciente entrar', async () => {
      simularBackEnd({ entrada: { status: 409, corpo: { mensagem: 'Fila fechada' } } })
      abrirComoPaciente()

      fireEvent.click(await screen.findByRole('button', { name: 'Entrar na fila de Clínico' }))

      expect(await screen.findByRole('alert')).toHaveTextContent('Fila fechada')
      expect(screen.queryByRole('heading', { name: 'Sua senha' })).not.toBeInTheDocument()
    })

    it('avisa quando o paciente já tem uma senha ativa no serviço', async () => {
      simularBackEnd({ entrada: { status: 409, corpo: { mensagem: 'Você já tem uma senha ativa neste serviço' } } })
      abrirComoPaciente()

      fireEvent.click(await screen.findByRole('button', { name: 'Entrar na fila de Clínico' }))

      expect(await screen.findByRole('alert')).toHaveTextContent('Você já tem uma senha ativa neste serviço')
    })

    it('avisa quando não há serviços disponíveis', async () => {
      simularBackEnd({ servicos: [] })
      abrirComoPaciente()

      expect(await screen.findByText('Nenhum serviço disponível hoje.')).toBeInTheDocument()
    })
  })

  describe('US07 - Ver minha posição', () => {
    // CT-US07-01 - Posição exibida
    // Dado que estou na fila / Quando abro a tela da fila
    // Então vejo minha senha, minha posição e quantas pessoas faltam
    it('CT-US07-01: mostra a senha, a posição e quantas pessoas estão na frente', async () => {
      const fetch = simularBackEnd({ senhas: [{ status: 200, corpo: [SENHA_CLINICO] }] })
      abrirComoPaciente()

      expect(await screen.findByRole('heading', { name: 'Sua senha' })).toBeInTheDocument()
      expect(screen.getByText('6')).toBeInTheDocument()
      expect(screen.getByText('3º')).toBeInTheDocument()
      expect(screen.getByText('2 pessoas na sua frente')).toBeInTheDocument()
      expect(screen.getByText(/Faça o check-in na recepção/)).toBeInTheDocument()

      // Já está na fila do Clínico: não dá para entrar de novo.
      const botao = await screen.findByRole('button', { name: 'Entrar na fila de Clínico' })
      expect(botao).toBeDisabled()
      expect(botao).toHaveTextContent('Na fila')

      const chamada = fetch.mock.calls.find(([url, opcoes]) => url === '/api/paciente/senhas' && !opcoes?.method)
      const cabecalhos = chamada?.[1]?.headers as Record<string, string>
      expect(cabecalhos.Authorization).toBe('Bearer token-paciente')
    })

    it('avisa quando o paciente é o próximo ou tem uma pessoa na frente', async () => {
      simularBackEnd({
        senhas: [{
          status: 200,
          corpo: [
            { ...SENHA_CLINICO, posicao: 1, pessoasAFrente: 0 },
            { ...SENHA_CLINICO, id: 10, numero: 2, servico: 'Odontologia', posicao: 2, pessoasAFrente: 1 },
          ],
        }],
      })
      abrirComoPaciente()

      expect(await screen.findByText('Você é o próximo da fila')).toBeInTheDocument()
      expect(screen.getByText('1 pessoa na sua frente')).toBeInTheDocument()
      expect(screen.getAllByRole('heading', { name: 'Sua senha' })).toHaveLength(2)
    })

    it('mostra que o check-in já foi feito', async () => {
      simularBackEnd({ senhas: [{ status: 200, corpo: [{ ...SENHA_CLINICO, status: 'PRESENTE' }] }] })
      abrirComoPaciente()

      expect(await screen.findByText(/Check-in feito\. Aguarde ser chamado\./)).toBeInTheDocument()
    })

    // CT-US07-02 - Não estou na fila
    // Dado que não tenho senha ativa / Quando abro a tela da fila
    // Então vejo a opção "Entrar na fila"
    it('CT-US07-02: sem senha ativa, mostra a opção "Entrar na fila"', async () => {
      simularBackEnd()
      abrirComoPaciente()

      const botao = await screen.findByRole('button', { name: 'Entrar na fila de Clínico' })
      expect(botao).toHaveTextContent('Entrar na fila')
      expect(botao).toBeEnabled()
      expect(screen.queryByRole('heading', { name: 'Sua senha' })).not.toBeInTheDocument()
    })

    it('atualiza a posição quando o paciente pede', async () => {
      simularBackEnd({
        senhas: [
          { status: 200, corpo: [SENHA_CLINICO] },
          { status: 200, corpo: [{ ...SENHA_CLINICO, posicao: 2, pessoasAFrente: 1 }] },
        ],
      })
      abrirComoPaciente()

      expect(await screen.findByText('2 pessoas na sua frente')).toBeInTheDocument()
      fireEvent.click(screen.getByRole('button', { name: 'Atualizar posição' }))

      expect(await screen.findByText('1 pessoa na sua frente')).toBeInTheDocument()
      expect(screen.getByText('2º')).toBeInTheDocument()
    })

    it('se a posição não carregar, avisa e continua mostrando os serviços', async () => {
      simularBackEnd({ senhas: [{ status: 500, corpo: {} }] })
      abrirComoPaciente()

      expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível carregar a sua posição na fila')
      expect(await screen.findByText('Clínico')).toBeInTheDocument()
    })
  })

  it('volta para o login quando a sessão expira', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('{}', { status: 401 }))
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
