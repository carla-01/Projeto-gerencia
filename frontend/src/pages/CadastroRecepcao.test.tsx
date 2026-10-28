import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'

/**
 * Cadastro de paciente pela recepção (US05 - SCRUM-40), pela tela.
 * O back-end é simulado: o fetch devolve o que o POST /api/recepcao/pacientes devolveria.
 * Só com dados fictícios.
 */

/** Faz o fetch responder com o status e o corpo informados (uma resposta nova a cada chamada). */
function simularResposta(status: number, corpo: object) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async () => {
    return new Response(JSON.stringify(corpo), {
      status: status,
      headers: { 'Content-Type': 'application/json' },
    })
  })
}

function abrirTelaComoRecepcionista() {
  localStorage.setItem('filasus.token', 'token-recepcionista')
  localStorage.setItem('filasus.perfil', 'RECEPCIONISTA')
  render(
    <MemoryRouter initialEntries={['/recepcao/cadastrar']}>
      <App />
    </MemoryRouter>,
  )
}

function digitar(rotulo: string, valor: string) {
  fireEvent.change(screen.getByLabelText(rotulo), { target: { value: valor } })
}

/** Preenche o formulário com um paciente fictício. */
function preencherPaciente() {
  digitar('Nome completo', 'Antônio Lima')
  digitar('CPF', '52998224725')
  digitar('Cartão SUS', '700123456789012')
  digitar('Telefone (opcional)', '8333330000')
  fireEvent.change(screen.getByLabelText('Necessidade de prioridade'), { target: { value: 'IDOSO' } })
}

function marcarConsentimento() {
  fireEvent.click(screen.getByLabelText('Paciente leu (ou ouviu) o termo e deu consentimento verbal'))
}

function clicarCadastrar() {
  fireEvent.click(screen.getByRole('button', { name: 'Cadastrar paciente' }))
}

describe('US05 - Recepção cadastra paciente', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('mostra o formulário do protótipo R2 com o menu da recepção', () => {
    abrirTelaComoRecepcionista()

    expect(screen.getByRole('heading', { name: 'Cadastrar paciente' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Cadastrar paciente' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Fila do dia' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'CPF já cadastrado?' })).toBeInTheDocument()
  })

  it('coloca as máscaras de CPF, cartão SUS e telefone', () => {
    abrirTelaComoRecepcionista()
    preencherPaciente()

    expect(screen.getByLabelText('CPF')).toHaveValue('529.982.247-25')
    expect(screen.getByLabelText('Cartão SUS')).toHaveValue('700 1234 5678 9012')
    expect(screen.getByLabelText('Telefone (opcional)')).toHaveValue('(83) 3333-0000')
  })

  // CT-US05-01 - Cadastro presencial
  // Dado que estou logado como recepcionista
  // Quando cadastro o paciente e registro o consentimento verbal
  // Então o paciente fica disponível para entrar na fila
  describe('CT-US05-01 - Cadastro presencial', () => {
    it('envia o cadastro com o consentimento verbal e mostra o paciente cadastrado', async () => {
      const fetch = simularResposta(201, {
        pacienteId: 7,
        nome: 'Antônio Lima',
        cpfMascarado: '529.***.***-25',
        prioridade: 'IDOSO',
        cadastradoEm: '2026-10-28T13:00:00Z',
        jaCadastrado: false,
        versaoTermo: '1.0',
        consentimentoEm: '2026-10-28T13:00:00Z',
      })
      abrirTelaComoRecepcionista()

      preencherPaciente()
      marcarConsentimento()
      clicarCadastrar()

      expect(await screen.findByRole('heading', { name: 'Paciente cadastrado' })).toBeInTheDocument()
      expect(screen.getByText(/529\.\*\*\*\.\*\*\*-25/)).toBeInTheDocument()

      // Confere o que foi enviado: só números, com o token da recepcionista.
      expect(fetch).toHaveBeenCalledTimes(1)
      const [url, opcoes] = fetch.mock.calls[0]
      expect(url).toBe('/api/recepcao/pacientes')
      expect(opcoes?.method).toBe('POST')
      const cabecalhos = opcoes?.headers as Record<string, string>
      expect(cabecalhos.Authorization).toBe('Bearer token-recepcionista')
      expect(JSON.parse(String(opcoes?.body))).toEqual({
        nome: 'Antônio Lima',
        cpf: '52998224725',
        cartaoSus: '700123456789012',
        telefone: '8333330000',
        prioridade: 'IDOSO',
        consentimentoVerbal: true,
      })

      // O formulário fica limpo para o próximo paciente.
      expect(screen.getByLabelText('Nome completo')).toHaveValue('')
    })

    it('sem o consentimento verbal não envia e avisa a recepcionista', () => {
      const fetch = simularResposta(201, {})
      abrirTelaComoRecepcionista()

      preencherPaciente()
      clicarCadastrar()

      expect(screen.getByText('Registre o consentimento verbal do paciente antes de cadastrar.')).toBeInTheDocument()
      expect(fetch).not.toHaveBeenCalled()
    })

    it('com CPF inválido não envia e marca o campo', () => {
      const fetch = simularResposta(201, {})
      abrirTelaComoRecepcionista()

      preencherPaciente()
      digitar('CPF', '12345678900')
      marcarConsentimento()
      clicarCadastrar()

      expect(screen.getByText('CPF inválido')).toBeInTheDocument()
      expect(fetch).not.toHaveBeenCalled()
    })
  })

  // CT-US05-02 - Paciente já cadastrado
  // Dado que o CPF já existe na base
  // Quando tento cadastrar de novo
  // Então o sistema abre o cadastro existente em vez de duplicar
  describe('CT-US05-02 - Paciente já cadastrado', () => {
    it('mostra o cadastro existente em vez de dizer que cadastrou', async () => {
      simularResposta(200, {
        pacienteId: 3,
        nome: 'Antônio Lima',
        cpfMascarado: '529.***.***-25',
        prioridade: 'IDOSO',
        cadastradoEm: '2026-10-21T13:00:00Z',
        jaCadastrado: true,
        versaoTermo: '1.0',
        consentimentoEm: '2026-10-21T13:00:00Z',
      })
      abrirTelaComoRecepcionista()

      preencherPaciente()
      marcarConsentimento()
      clicarCadastrar()

      expect(await screen.findByRole('heading', { name: 'Paciente encontrado' })).toBeInTheDocument()
      expect(screen.getByText(/cadastrado em 21\/10\/2026/)).toBeInTheDocument()
      expect(screen.queryByRole('heading', { name: 'Paciente cadastrado' })).not.toBeInTheDocument()
    })
  })

  it('erro de campo vindo do back-end aparece no campo', async () => {
    simularResposta(400, { campo: 'cartaoSus', mensagem: 'O cartão SUS tem 15 números' })
    abrirTelaComoRecepcionista()

    preencherPaciente()
    marcarConsentimento()
    clicarCadastrar()

    expect(await screen.findByText('O cartão SUS tem 15 números')).toBeInTheDocument()
  })

  it('outro perfil não abre a tela de cadastro da recepção', () => {
    localStorage.setItem('filasus.token', 'token-paciente')
    localStorage.setItem('filasus.perfil', 'PACIENTE')
    render(
      <MemoryRouter initialEntries={['/recepcao/cadastrar']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Acesso não permitido' })).toBeInTheDocument()
  })
})
