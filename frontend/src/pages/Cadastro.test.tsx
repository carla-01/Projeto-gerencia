import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { cpfValido, mascararCpf } from '../services/cpf'

/**
 * Cenários Gherkin da US04 - Autocadastro do paciente com consentimento (SCRUM-39), pela tela.
 * O back-end é simulado: o fetch devolve a mesma resposta que o POST /pacientes/autocadastro devolveria.
 * Só com dados fictícios.
 */

const FINALIDADE =
  'Para se cadastrar, aceite o termo. Seus dados são usados só para organizar ' +
  'a fila de atendimento da UBS, e você pode pedir a exclusão deles quando quiser.'

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

function abrirTelaDeCadastro() {
  render(
    <MemoryRouter initialEntries={['/cadastro']}>
      <App />
    </MemoryRouter>,
  )
}

/** Preenche todos os campos obrigatórios com dados fictícios. */
function preencherDados(cpf: string) {
  fireEvent.change(screen.getByLabelText('Nome completo'), { target: { value: 'Maria da Silva' } })
  fireEvent.change(screen.getByLabelText('CPF'), { target: { value: cpf } })
  fireEvent.change(screen.getByLabelText('Cartão SUS'), { target: { value: '898001234567890' } })
  fireEvent.change(screen.getByLabelText('E-mail'), { target: { value: 'maria@email.com' } })
  fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'senhaForte123' } })
}

function aceitarTermo() {
  fireEvent.click(screen.getByLabelText('Li e aceito o termo de consentimento'))
}

function concluir() {
  fireEvent.click(screen.getByRole('button', { name: 'Criar conta' }))
}

describe('US04 - Autocadastro do paciente com consentimento', () => {
  afterEach(() => {
    // O Vitest do projeto não limpa a tela sozinho entre um teste e outro.
    cleanup()
    vi.restoreAllMocks()
  })

  // CT-US04-01 - Cadastro com sucesso
  // Dado que estou na tela de cadastro
  // Quando preencho os dados obrigatórios e aceito o termo LGPD
  // Então minha conta é criada e o aceite fica registrado com data e hora
  describe('CT-US04-01 - Cadastro com sucesso', () => {
    it('cria a conta e mostra o aceite registrado com data e hora', async () => {
      simularResposta(201, {
        pacienteId: 1,
        nome: 'Maria da Silva',
        prioridade: 'NENHUMA',
        versaoTermo: '1.0',
        aceitoEm: '2026-10-28T20:24:26Z',
      })
      abrirTelaDeCadastro()

      preencherDados('529.982.247-25')
      aceitarTermo()
      concluir()

      expect(await screen.findByRole('heading', { name: 'Conta criada' })).toBeInTheDocument()
      const dataHora = new Date('2026-10-28T20:24:26Z').toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
      expect(screen.getByText(`Aceite do termo (versão 1.0) registrado em ${dataHora}.`)).toBeInTheDocument()
      expect(screen.getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/login')
    })

    it('envia o aceite e o CPF só com os números', async () => {
      const fetch = simularResposta(201, { pacienteId: 1, nome: 'Maria da Silva', versaoTermo: '1.0', aceitoEm: '2026-10-28T20:24:26Z' })
      abrirTelaDeCadastro()

      preencherDados('529.982.247-25')
      aceitarTermo()
      concluir()
      await screen.findByRole('heading', { name: 'Conta criada' })

      const [url, opcoes] = fetch.mock.calls[0]
      expect(url).toBe('/pacientes/autocadastro')
      expect(JSON.parse(opcoes?.body as string)).toMatchObject({
        cpf: '52998224725',
        cartaoSus: '898001234567890',
        email: 'maria@email.com',
        telefone: null,
        aceiteTermo: true,
      })
    })
  })

  // CT-US04-02 - Sem consentimento
  // Dado que preenchi os dados
  // Quando tento concluir sem marcar o aceite do termo
  // Então o cadastro não é concluído e vejo a finalidade do uso dos dados
  describe('CT-US04-02 - Sem consentimento', () => {
    it('não envia o cadastro e mostra a finalidade do uso dos dados', () => {
      const fetch = vi.spyOn(globalThis, 'fetch')
      abrirTelaDeCadastro()

      preencherDados('529.982.247-25')
      concluir()

      expect(screen.getByText(FINALIDADE)).toBeInTheDocument()
      expect(screen.getByRole('heading', { name: 'Criar conta' })).toBeInTheDocument()
      expect(fetch).not.toHaveBeenCalled()
    })

    it('mostra a finalidade também quando o servidor recusa por falta de aceite', async () => {
      simularResposta(400, { campo: 'aceiteTermo', mensagem: FINALIDADE })
      abrirTelaDeCadastro()

      preencherDados('529.982.247-25')
      aceitarTermo()
      concluir()

      expect(await screen.findByText(FINALIDADE)).toBeInTheDocument()
    })
  })

  // CT-US04-03 - CPF inválido
  // Dado que estou na tela de cadastro
  // Quando informo um CPF com dígito verificador errado
  // Então vejo "CPF inválido" no campo
  describe('CT-US04-03 - CPF inválido', () => {
    it('mostra "CPF inválido" no campo e não envia', () => {
      const fetch = vi.spyOn(globalThis, 'fetch')
      abrirTelaDeCadastro()

      preencherDados('529.982.247-00')
      aceitarTermo()
      concluir()

      expect(screen.getByLabelText('CPF')).toHaveAccessibleDescription('CPF inválido')
      expect(fetch).not.toHaveBeenCalled()
    })

    it('mostra "CPF inválido" no campo quando o servidor recusa o CPF', async () => {
      simularResposta(400, { campo: 'cpf', mensagem: 'CPF inválido' })
      abrirTelaDeCadastro()

      preencherDados('529.982.247-25')
      aceitarTermo()
      concluir()

      expect(await screen.findByText('CPF inválido')).toBeInTheDocument()
      expect(screen.getByLabelText('CPF')).toHaveAccessibleDescription('CPF inválido')
    })

    it('confere os dígitos verificadores', () => {
      expect(mascararCpf('52998224725')).toBe('529.982.247-25')
      expect(cpfValido('529.982.247-25')).toBe(true)
      expect(cpfValido('529.982.247-00')).toBe(false)
      expect(cpfValido('111.111.111-11')).toBe(false)
    })
  })

  describe('CPF ou e-mail já cadastrado', () => {
    it('mostra a mensagem do servidor', async () => {
      simularResposta(409, { mensagem: 'Já existe um cadastro com este CPF' })
      abrirTelaDeCadastro()

      preencherDados('529.982.247-25')
      aceitarTermo()
      concluir()

      expect(await screen.findByRole('alert')).toHaveTextContent('Já existe um cadastro com este CPF')
    })
  })
})
