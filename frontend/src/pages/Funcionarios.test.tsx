import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import Funcionarios from './Funcionarios'
import {
  ErroFuncionario,
  cadastrarFuncionario,
  listarFuncionarios,
  listarSetores,
} from '../services/funcionarios'
import type { Funcionario } from '../services/funcionarios'

/** US03 - Gestor cadastra funcionários (SCRUM-152), pela tela. Dados fictícios; a API é simulada. */

vi.mock('../services/funcionarios', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/funcionarios')>()),
  cadastrarFuncionario: vi.fn(),
  listarFuncionarios: vi.fn(),
  listarSetores: vi.fn(),
}))

const cadastrarMock = vi.mocked(cadastrarFuncionario)
const listarFuncionariosMock = vi.mocked(listarFuncionarios)
const listarSetoresMock = vi.mocked(listarSetores)

const ENFERMEIRA: Funcionario = {
  id: 7,
  nome: 'Enfermeira Fictícia',
  email: 'enfermeira@exemplo.test',
  perfil: 'PROFISSIONAL',
  ativo: true,
  setorId: 2,
  setor: 'Enfermagem',
}

function abrirTela() {
  render(
    <MemoryRouter>
      <Funcionarios />
    </MemoryRouter>,
  )
}

function preencher(rotulo: string, valor: string) {
  fireEvent.change(screen.getByLabelText(rotulo), { target: { value: valor } })
}

/** Preenche o formulário de um profissional da Enfermagem (setor 2). */
async function preencherProfissional() {
  preencher('Nome', 'Enfermeira Fictícia')
  preencher('E-mail', 'enfermeira@exemplo.test')
  preencher('CPF', '52998224725')
  preencher('Cartão SUS', '898001234567891')
  preencher('Papel', 'PROFISSIONAL')
  // As opções de setor chegam da API.
  await screen.findByRole('option', { name: 'Enfermagem' })
  preencher('Setor', '2')
  preencher('Senha inicial', 'inicial123')
}

describe('US03 - Funcionários do gestor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    listarSetoresMock.mockResolvedValue([
      { id: 1, nome: 'Clínico' },
      { id: 2, nome: 'Enfermagem' },
    ])
    listarFuncionariosMock.mockResolvedValue([])
  })

  // CT-US03-01 - Cadastro de funcionário
  // Dado que estou logado como gestor
  // Quando cadastro um profissional com nome, e-mail, setor e papel
  // Então ele recebe acesso e aparece na lista de usuários
  it('CT-US03-01: cadastra o profissional com o setor e mostra na lista', async () => {
    cadastrarMock.mockResolvedValue(ENFERMEIRA)
    abrirTela()
    expect(await screen.findByText('Nenhum funcionário cadastrado ainda.')).toBeInTheDocument()

    await preencherProfissional()
    listarFuncionariosMock.mockResolvedValue([ENFERMEIRA])
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar funcionário' }))

    await waitFor(() => {
      expect(cadastrarMock).toHaveBeenCalledWith({
        nome: 'Enfermeira Fictícia',
        email: 'enfermeira@exemplo.test',
        cpf: '529.982.247-25',
        cartaoSus: '898001234567891',
        perfil: 'PROFISSIONAL',
        servicoId: 2,
        senhaInicial: 'inicial123',
      })
    })
    expect(await screen.findByRole('status')).toHaveTextContent('Enfermeira Fictícia foi cadastrado(a)')

    const linha = (await screen.findByRole('cell', { name: 'enfermeira@exemplo.test' })).closest('tr')!
    expect(within(linha).getByText('Profissional de saúde')).toBeInTheDocument()
    expect(within(linha).getByText('Enfermagem')).toBeInTheDocument()
    // O formulário fica limpo para o próximo cadastro.
    expect(screen.getByLabelText('Nome')).toHaveValue('')
  })

  // CT-US03-02 - E-mail duplicado
  // Dado que já existe um usuário com o e-mail informado
  // Quando tento salvar o cadastro
  // Então vejo "E-mail já cadastrado" e nada é gravado
  it('CT-US03-02: mostra "E-mail já cadastrado" e não muda a lista', async () => {
    cadastrarMock.mockRejectedValue(new ErroFuncionario('E-mail já cadastrado'))
    abrirTela()
    await screen.findByText('Nenhum funcionário cadastrado ainda.')

    await preencherProfissional()
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar funcionário' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail já cadastrado')
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    expect(listarFuncionariosMock).toHaveBeenCalledTimes(1)
    // Os dados continuam no formulário para a correção.
    expect(screen.getByLabelText('E-mail')).toHaveValue('enfermeira@exemplo.test')
  })

  it('mostra o erro do back-end no campo indicado', async () => {
    cadastrarMock.mockRejectedValue(new ErroFuncionario('CPF já cadastrado', 'cpf'))
    abrirTela()

    await preencherProfissional()
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar funcionário' }))

    expect(await screen.findByText('CPF já cadastrado')).toBeInTheDocument()
    expect(screen.getByLabelText('CPF')).toHaveAttribute('aria-invalid', 'true')
  })

  it('só pede o setor para o profissional de saúde', async () => {
    abrirTela()

    expect(screen.queryByLabelText('Setor')).not.toBeInTheDocument()
    preencher('Papel', 'PROFISSIONAL')
    expect(screen.getByLabelText('Setor')).toBeInTheDocument()
    preencher('Papel', 'RECEPCIONISTA')
    expect(screen.queryByLabelText('Setor')).not.toBeInTheDocument()
  })

  it('cadastra recepcionista sem setor', async () => {
    cadastrarMock.mockResolvedValue({ ...ENFERMEIRA, perfil: 'RECEPCIONISTA', setorId: null, setor: null })
    abrirTela()

    preencher('Nome', 'Recepcionista Fictícia')
    preencher('E-mail', 'recepcao@exemplo.test')
    preencher('CPF', '11144477735')
    preencher('Cartão SUS', '898001234567892')
    preencher('Papel', 'RECEPCIONISTA')
    preencher('Senha inicial', 'inicial123')
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar funcionário' }))

    await waitFor(() => {
      expect(cadastrarMock).toHaveBeenCalledWith(expect.objectContaining({ perfil: 'RECEPCIONISTA', servicoId: null }))
    })
  })

  it('confere os campos antes de enviar e não chama a API', async () => {
    abrirTela()

    preencher('CPF', '12345678900')
    preencher('Cartão SUS', '123')
    preencher('Papel', 'PROFISSIONAL')
    preencher('Senha inicial', '1234567')
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar funcionário' }))

    expect(screen.getByText('Informe o nome.')).toBeInTheDocument()
    expect(screen.getByText('E-mail inválido')).toBeInTheDocument()
    expect(screen.getByText('CPF inválido')).toBeInTheDocument()
    expect(screen.getByText('O cartão SUS tem 15 números')).toBeInTheDocument()
    expect(screen.getByText('Escolha o setor do profissional.')).toBeInTheDocument()
    expect(screen.getByText('A senha inicial deve ter no mínimo 8 caracteres.')).toBeInTheDocument()
    expect(cadastrarMock).not.toHaveBeenCalled()
  })

  it('mostra a lista de usuários da UBS com o setor de cada profissional', async () => {
    listarFuncionariosMock.mockResolvedValue([
      { id: 1, nome: 'Ana Recepcionista', email: 'ana@exemplo.test', perfil: 'RECEPCIONISTA', ativo: true, setorId: null, setor: null },
      ENFERMEIRA,
    ])
    abrirTela()

    expect(await screen.findByRole('cell', { name: 'Ana Recepcionista' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'Recepcionista' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'Enfermagem' })).toBeInTheDocument()
  })

  it('a área do gestor tem o menu com Funcionários e Configurar filas', async () => {
    localStorage.setItem('filasus.token', 'token-de-teste')
    localStorage.setItem('filasus.perfil', 'GESTOR')
    render(
      <MemoryRouter initialEntries={['/gestor']}>
        <App />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: 'Funcionários da UBS' })).toBeInTheDocument()
    const menu = screen.getByRole('navigation', { name: 'Menu do gestor' })
    expect(within(menu).getByRole('link', { name: 'Funcionários' })).toHaveAttribute('href', '/gestor')
    expect(within(menu).getByRole('link', { name: 'Configurar filas' })).toHaveAttribute('href', '/gestor/configurar-filas')
  })

  it('outros perfis não abrem a área do gestor', () => {
    localStorage.setItem('filasus.token', 'token-de-teste')
    localStorage.setItem('filasus.perfil', 'RECEPCIONISTA')
    render(
      <MemoryRouter initialEntries={['/gestor']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Acesso não permitido' })).toBeInTheDocument()
    expect(listarFuncionariosMock).not.toHaveBeenCalled()
  })
})
