import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { cpfValido, mascararCpf, somenteNumeros } from '../services/cpf'
import {
  ErroFuncionario,
  cadastrarFuncionario,
  listarFuncionarios,
  listarSetores,
} from '../services/funcionarios'
import type { Funcionario, PapelFuncionario, Setor } from '../services/funcionarios'
import type { Perfil } from '../services/auth'
import './ConfigurarFilas.css'
import './Funcionarios.css'

/**
 * Cadastro e lista de funcionários pelo gestor (US03 - SCRUM-152).
 * Cenário 1: o funcionário cadastrado recebe acesso e aparece na lista.
 * Cenário 2: e-mail repetido mostra "E-mail já cadastrado" e nada é gravado.
 */

type Campos = {
  nome: string
  email: string
  cpf: string
  cartaoSus: string
  perfil: '' | PapelFuncionario
  servicoId: string
  senhaInicial: string
}

type Campo = keyof Campos
type Erros = Partial<Record<Campo, string>>

const CAMPOS_VAZIOS: Campos = {
  nome: '',
  email: '',
  cpf: '',
  cartaoSus: '',
  perfil: '',
  servicoId: '',
  senhaInicial: '',
}

const NOME_DO_PAPEL: Record<Perfil, string> = {
  PACIENTE: 'Paciente',
  RECEPCIONISTA: 'Recepcionista',
  PROFISSIONAL: 'Profissional de saúde',
  GESTOR: 'Gestor',
}

/** Confere os campos antes de enviar. O back-end confere de novo. */
function validar(campos: Campos): Erros {
  const erros: Erros = {}
  if (campos.nome.trim() === '') {
    erros.nome = 'Informe o nome.'
  }
  if (!/^\S+@\S+\.\S+$/.test(campos.email.trim())) {
    erros.email = 'E-mail inválido'
  }
  if (!cpfValido(campos.cpf)) {
    erros.cpf = 'CPF inválido'
  }
  if (somenteNumeros(campos.cartaoSus).length !== 15) {
    erros.cartaoSus = 'O cartão SUS tem 15 números'
  }
  if (campos.perfil === '') {
    erros.perfil = 'Escolha o papel.'
  }
  if (campos.perfil === 'PROFISSIONAL' && campos.servicoId === '') {
    erros.servicoId = 'Escolha o setor do profissional.'
  }
  if (campos.senhaInicial.length < 8) {
    erros.senhaInicial = 'A senha inicial deve ter no mínimo 8 caracteres.'
  }
  return erros
}

export default function Funcionarios() {
  const [campos, setCampos] = useState<Campos>(CAMPOS_VAZIOS)
  const [erros, setErros] = useState<Erros>({})
  const [erroGeral, setErroGeral] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [enviando, setEnviando] = useState(false)

  const [setores, setSetores] = useState<Setor[]>([])
  const [funcionarios, setFuncionarios] = useState<Funcionario[]>([])
  const [carregandoLista, setCarregandoLista] = useState(true)
  const [erroLista, setErroLista] = useState('')

  async function carregarLista() {
    setErroLista('')
    try {
      setFuncionarios(await listarFuncionarios())
    } catch (e) {
      setErroLista(e instanceof Error ? e.message : 'Não foi possível carregar a lista de usuários. Tente de novo.')
    } finally {
      setCarregandoLista(false)
    }
  }

  useEffect(() => {
    carregarLista()
    listarSetores()
      .then(setSetores)
      .catch((e) => setErroGeral(e instanceof Error ? e.message : 'Não foi possível carregar os setores.'))
  }, [])

  function alterar(campo: Campo, valor: string) {
    const novos = { ...campos, [campo]: valor }
    // Só o profissional tem setor: ao trocar o papel, o setor escolhido sai.
    if (campo === 'perfil' && valor !== 'PROFISSIONAL') {
      novos.servicoId = ''
    }
    setCampos(novos)
    setErros({ ...erros, [campo]: undefined })
    setErroGeral('')
    setSucesso('')
  }

  async function cadastrar(evento: FormEvent) {
    evento.preventDefault()
    setErroGeral('')
    setSucesso('')

    const errosEncontrados = validar(campos)
    setErros(errosEncontrados)
    if (Object.keys(errosEncontrados).length > 0) {
      return
    }

    setEnviando(true)
    try {
      const salvo = await cadastrarFuncionario({
        nome: campos.nome.trim(),
        email: campos.email.trim(),
        cpf: campos.cpf,
        cartaoSus: somenteNumeros(campos.cartaoSus),
        perfil: campos.perfil as PapelFuncionario,
        servicoId: campos.perfil === 'PROFISSIONAL' ? Number(campos.servicoId) : null,
        senhaInicial: campos.senhaInicial,
      })
      setCampos(CAMPOS_VAZIOS)
      setSucesso(`${salvo.nome} foi cadastrado(a) e já pode entrar com o e-mail e a senha inicial.`)
      await carregarLista()
    } catch (e) {
      if (e instanceof ErroFuncionario && e.campo !== null && e.campo in CAMPOS_VAZIOS) {
        setErros({ [e.campo]: e.message })
      } else {
        setErroGeral(e instanceof Error ? e.message : 'Não foi possível cadastrar o funcionário agora. Tente de novo.')
      }
    } finally {
      setEnviando(false)
    }
  }

  /** Liga o campo à mensagem de erro dele, para leitores de tela. */
  function acessibilidade(campo: Campo) {
    if (erros[campo] === undefined) {
      return {}
    }
    return { 'aria-invalid': true, 'aria-describedby': `erro-${campo}` }
  }

  function erroDoCampo(campo: Campo) {
    if (erros[campo] === undefined) {
      return null
    }
    return (
      <p id={`erro-${campo}`} className="gestor-config-mensagem gestor-config-erro">
        {erros[campo]}
      </p>
    )
  }

  return (
    <main className="gestor-config gestor-funcionarios">
      <header className="gestor-config-cabecalho">
        <p className="gestor-config-rotulo">Área do gestor</p>
        <h1>Funcionários da UBS</h1>
        <p>Cadastre recepcionistas e profissionais de saúde para que a equipe tenha acesso ao sistema.</p>
      </header>

      <section className="gestor-funcionarios-bloco" aria-labelledby="titulo-cadastro">
        <h2 id="titulo-cadastro">Cadastrar funcionário</h2>

        <form className="gestor-config-form" onSubmit={cadastrar} noValidate>
          <div className="gestor-config-linha">
            <div className="gestor-config-campo">
              <label htmlFor="nome">Nome</label>
              <input id="nome" type="text" autoComplete="off" value={campos.nome}
                onChange={(e) => alterar('nome', e.target.value)} {...acessibilidade('nome')} />
              {erroDoCampo('nome')}
            </div>
            <div className="gestor-config-campo">
              <label htmlFor="email">E-mail</label>
              <input id="email" type="email" autoComplete="off" value={campos.email}
                onChange={(e) => alterar('email', e.target.value)} {...acessibilidade('email')} />
              {erroDoCampo('email')}
            </div>
          </div>

          <div className="gestor-config-linha">
            <div className="gestor-config-campo">
              <label htmlFor="cpf">CPF</label>
              <input id="cpf" type="text" inputMode="numeric" placeholder="000.000.000-00" value={campos.cpf}
                onChange={(e) => alterar('cpf', mascararCpf(e.target.value))} {...acessibilidade('cpf')} />
              {erroDoCampo('cpf')}
            </div>
            <div className="gestor-config-campo">
              <label htmlFor="cartaoSus">Cartão SUS</label>
              <input id="cartaoSus" type="text" inputMode="numeric" placeholder="15 números" value={campos.cartaoSus}
                onChange={(e) => alterar('cartaoSus', somenteNumeros(e.target.value).slice(0, 15))}
                {...acessibilidade('cartaoSus')} />
              {erroDoCampo('cartaoSus')}
            </div>
          </div>

          <div className="gestor-config-linha">
            <div className="gestor-config-campo">
              <label htmlFor="perfil">Papel</label>
              <select id="perfil" value={campos.perfil}
                onChange={(e) => alterar('perfil', e.target.value)} {...acessibilidade('perfil')}>
                <option value="">Escolha o papel</option>
                <option value="RECEPCIONISTA">Recepcionista</option>
                <option value="PROFISSIONAL">Profissional de saúde</option>
              </select>
              {erroDoCampo('perfil')}
            </div>
            {campos.perfil === 'PROFISSIONAL' && (
              <div className="gestor-config-campo">
                <label htmlFor="servicoId">Setor</label>
                <select id="servicoId" value={campos.servicoId}
                  onChange={(e) => alterar('servicoId', e.target.value)} {...acessibilidade('servicoId')}>
                  <option value="">Escolha o setor</option>
                  {setores.map((setor) => (
                    <option key={setor.id} value={setor.id}>{setor.nome}</option>
                  ))}
                </select>
                {erroDoCampo('servicoId')}
              </div>
            )}
          </div>

          <div className="gestor-config-campo">
            <label htmlFor="senhaInicial">Senha inicial</label>
            <input id="senhaInicial" type="password" autoComplete="new-password" value={campos.senhaInicial}
              onChange={(e) => alterar('senhaInicial', e.target.value)} {...acessibilidade('senhaInicial')} />
            {erroDoCampo('senhaInicial') ?? (
              <p className="gestor-funcionarios-dica">Mínimo de 8 caracteres. Entregue a senha ao funcionário pessoalmente.</p>
            )}
          </div>

          {erroGeral !== '' && <p className="gestor-config-mensagem gestor-config-erro" role="alert">{erroGeral}</p>}
          {sucesso !== '' && <p className="gestor-config-mensagem gestor-config-sucesso" role="status">{sucesso}</p>}

          <button type="submit" disabled={enviando}>
            {enviando ? 'Cadastrando...' : 'Cadastrar funcionário'}
          </button>
        </form>
      </section>

      <section className="gestor-funcionarios-bloco" aria-labelledby="titulo-lista">
        <h2 id="titulo-lista">Usuários da UBS</h2>

        {carregandoLista && <p className="gestor-funcionarios-vazio">Carregando...</p>}
        {erroLista !== '' && <p className="gestor-config-mensagem gestor-config-erro" role="alert">{erroLista}</p>}
        {!carregandoLista && erroLista === '' && funcionarios.length === 0 && (
          <p className="gestor-funcionarios-vazio">Nenhum funcionário cadastrado ainda.</p>
        )}

        {funcionarios.length > 0 && (
          <div className="gestor-funcionarios-tabela">
            <table>
              <thead>
                <tr>
                  <th scope="col">Nome</th>
                  <th scope="col">E-mail</th>
                  <th scope="col">Papel</th>
                  <th scope="col">Setor</th>
                </tr>
              </thead>
              <tbody>
                {funcionarios.map((funcionario) => (
                  <tr key={funcionario.id}>
                    <td>{funcionario.nome}</td>
                    <td>{funcionario.email}</td>
                    <td>{NOME_DO_PAPEL[funcionario.perfil]}</td>
                    <td>{funcionario.setor ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  )
}
