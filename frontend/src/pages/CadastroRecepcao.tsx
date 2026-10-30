import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { fazerLogout } from '../services/auth'
import { cpfValido, mascararCpf, somenteNumeros } from '../services/cpf'
import { ErroCadastroRecepcao, cadastrarPaciente } from '../services/cadastroRecepcao'
import type {
  CampoCadastroRecepcao,
  DadosCadastroRecepcao,
  PacienteCadastrado,
  PrioridadeRecepcao,
} from '../services/cadastroRecepcao'
import './CadastroRecepcao.css'

const CONSENTIMENTO_OBRIGATORIO = 'Registre o consentimento verbal do paciente antes de cadastrar.'

const OPCOES_PRIORIDADE: { valor: PrioridadeRecepcao; texto: string }[] = [
  { valor: 'NENHUMA', texto: 'Sem prioridade' },
  { valor: 'IDOSO', texto: 'Idoso 60+' },
  { valor: 'GESTANTE', texto: 'Gestante' },
  { valor: 'PCD', texto: 'Pessoa com deficiência' },
]

const DADOS_VAZIOS: DadosCadastroRecepcao = {
  nome: '',
  cpf: '',
  cartaoSus: '',
  telefone: '',
  prioridade: 'NENHUMA',
  consentimentoVerbal: false,
}

type ErrosDosCampos = Partial<Record<CampoCadastroRecepcao, string>>

/** Coloca a máscara 898 0012 3456 7890 no cartão SUS (15 números). */
function mascararCartaoSus(texto: string): string {
  const numeros = somenteNumeros(texto).slice(0, 15)
  let resultado = numeros.slice(0, 3)
  if (numeros.length > 3) {
    resultado = resultado + ' ' + numeros.slice(3, 7)
  }
  if (numeros.length > 7) {
    resultado = resultado + ' ' + numeros.slice(7, 11)
  }
  if (numeros.length > 11) {
    resultado = resultado + ' ' + numeros.slice(11, 15)
  }
  return resultado
}

/** Coloca a máscara (83) 3333-0000 ou (83) 99999-0000 no telefone. */
function mascararTelefone(texto: string): string {
  const numeros = somenteNumeros(texto).slice(0, 11)
  if (numeros.length <= 2) {
    return numeros
  }
  if (numeros.length <= 6) {
    return '(' + numeros.slice(0, 2) + ') ' + numeros.slice(2)
  }
  // Celular tem 11 números (5 antes do traço); fixo tem 10 (4 antes do traço).
  let fimDoMeio = 6
  if (numeros.length === 11) {
    fimDoMeio = 7
  }
  return '(' + numeros.slice(0, 2) + ') ' + numeros.slice(2, fimDoMeio) + '-' + numeros.slice(fimDoMeio)
}

/** Confere os campos antes de enviar. Devolve só os campos com erro. */
function validar(dados: DadosCadastroRecepcao): ErrosDosCampos {
  const erros: ErrosDosCampos = {}

  if (dados.nome.trim() === '') {
    erros.nome = 'Informe o nome do paciente.'
  }
  if (!cpfValido(dados.cpf)) {
    erros.cpf = 'CPF inválido'
  }
  if (somenteNumeros(dados.cartaoSus).length !== 15) {
    erros.cartaoSus = 'O cartão SUS tem 15 números.'
  }
  const telefone = somenteNumeros(dados.telefone)
  if (telefone !== '' && telefone.length < 10) {
    erros.telefone = 'Informe o DDD e o número.'
  }
  if (!dados.consentimentoVerbal) {
    erros.consentimentoVerbal = CONSENTIMENTO_OBRIGATORIO
  }

  return erros
}

/** "2026-10-21T13:00:00Z" vira "21/10/2026". */
function formatarData(iso: string): string {
  return new Date(iso).toLocaleDateString('pt-BR')
}

/** "2026-10-21T13:00:00Z" vira "21/10/2026, 10:00" no horário do navegador. */
function formatarDataHora(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

function textoPrioridade(prioridade: PrioridadeRecepcao): string {
  for (const opcao of OPCOES_PRIORIDADE) {
    if (opcao.valor === prioridade) {
      return opcao.texto
    }
  }
  return prioridade
}

function IconeAlerta() {
  return (
    <svg className="cadrec-icone-alerta" viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path d="M12 7.5v5.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <circle cx="12" cy="16.5" r="1.2" fill="currentColor" />
    </svg>
  )
}

function IconeCheck() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function IconePessoa() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="8" r="4" fill="none" stroke="currentColor" strokeWidth="1.9" />
      <path d="M4 20c0-4 3.6-6 8-6s8 2 8 6" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
    </svg>
  )
}

function IconeBusca() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="11" cy="11" r="6.5" fill="none" stroke="currentColor" strokeWidth="1.9" />
      <path d="M16 16l4.5 4.5" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
    </svg>
  )
}

/**
 * Cadastro de paciente pela recepção (US05 - SCRUM-40), conforme o protótipo R2.
 *
 * A recepcionista cadastra quem não tem celular ou não sabe usar o sistema e
 * marca que o paciente deu o consentimento verbal (CT-US05-01).
 * Se o CPF já existir, aparece o cadastro existente em vez de criar outro (CT-US05-02).
 */
export default function CadastroRecepcao() {
  const navigate = useNavigate()
  const [dados, setDados] = useState<DadosCadastroRecepcao>(DADOS_VAZIOS)
  const [erros, setErros] = useState<ErrosDosCampos>({})
  const [erroGeral, setErroGeral] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [resultado, setResultado] = useState<PacienteCadastrado | null>(null)

  function alterar<Campo extends CampoCadastroRecepcao>(campo: Campo, valor: DadosCadastroRecepcao[Campo]) {
    setDados({ ...dados, [campo]: valor })
    // O erro do campo some assim que a recepcionista corrige.
    if (erros[campo] !== undefined) {
      const semEsteErro = { ...erros }
      delete semEsteErro[campo]
      setErros(semEsteErro)
    }
  }

  async function cadastrar(evento: FormEvent) {
    evento.preventDefault()
    setErroGeral('')
    setResultado(null)

    const errosEncontrados = validar(dados)
    setErros(errosEncontrados)
    if (Object.keys(errosEncontrados).length > 0) {
      return
    }

    setEnviando(true)
    try {
      const paciente = await cadastrarPaciente(dados)
      setResultado(paciente)
      // Cadastro novo: limpa o formulário para o próximo paciente.
      if (!paciente.jaCadastrado) {
        setDados(DADOS_VAZIOS)
      }
    } catch (e) {
      tratarErro(e)
    }
    setEnviando(false)
  }

  function tratarErro(e: unknown) {
    if (!(e instanceof ErroCadastroRecepcao)) {
      setErroGeral('Não foi possível concluir o cadastro agora. Tente de novo.')
      return
    }
    if (e.tipo === 'sem-login') {
      fazerLogout()
      navigate('/login', { replace: true })
      return
    }
    if (e.tipo === 'sem-permissao') {
      navigate('/acesso-negado', { replace: true })
      return
    }
    if (e.campo !== null) {
      setErros({ [e.campo]: e.message })
      return
    }
    setErroGeral(e.message)
  }

  function classeCampo(campo: CampoCadastroRecepcao): string {
    if (erros[campo] !== undefined) {
      return 'cadrec-campo cadrec-campo-erro'
    }
    return 'cadrec-campo'
  }

  function erroDoCampo(campo: CampoCadastroRecepcao) {
    const mensagem = erros[campo]
    if (mensagem === undefined) {
      return null
    }
    return (
      <p id={'erro-' + campo} className="cadrec-erro-campo">
        <IconeAlerta />
        {mensagem}
      </p>
    )
  }

  /** Liga o campo à mensagem de erro, para o leitor de tela anunciar. */
  function acessibilidade(campo: CampoCadastroRecepcao) {
    if (erros[campo] === undefined) {
      return {}
    }
    return { 'aria-invalid': true, 'aria-describedby': 'erro-' + campo }
  }

  let textoBotao = 'Cadastrar paciente'
  if (enviando) {
    textoBotao = 'Cadastrando...'
  }

  let classeConsentimento = 'cadrec-termo'
  if (dados.consentimentoVerbal) {
    classeConsentimento = 'cadrec-termo cadrec-termo-marcado'
  }
  if (erros.consentimentoVerbal !== undefined) {
    classeConsentimento = 'cadrec-termo cadrec-termo-erro'
  }

  return (
    <div className="cadrec">
      <header className="cadrec-cabecalho">
        <p className="cadrec-rotulo">Recepção</p>
        <h1>Cadastrar paciente</h1>
        <p>Para quem não tem celular ou não sabe usar o sistema</p>
      </header>

      <div className="cadrec-colunas">
        <form className="cadrec-cartao cadrec-form" onSubmit={cadastrar} noValidate>
          {erroGeral !== '' && (
            <div className="cadrec-alerta" role="alert">
              <IconeAlerta />
              {erroGeral}
            </div>
          )}

          <section className="cadrec-secao">
            <h2 className="cadrec-secao-titulo">
              <span className="cadrec-numero" aria-hidden="true">1</span>
              Dados do paciente
            </h2>

            <div className="cadrec-grupo">
              <label htmlFor="nome">Nome completo</label>
              <input
                id="nome"
                className={classeCampo('nome')}
                type="text"
                placeholder="Antônio Lima"
                autoComplete="off"
                value={dados.nome}
                onChange={(e) => alterar('nome', e.target.value)}
                {...acessibilidade('nome')}
              />
              {erroDoCampo('nome')}
            </div>

            <div className="cadrec-linha">
              <div className="cadrec-grupo">
                <label htmlFor="cpf">CPF</label>
                <input
                  id="cpf"
                  className={classeCampo('cpf')}
                  type="text"
                  inputMode="numeric"
                  placeholder="000.000.000-00"
                  autoComplete="off"
                  value={dados.cpf}
                  onChange={(e) => alterar('cpf', mascararCpf(e.target.value))}
                  {...acessibilidade('cpf')}
                />
                {erroDoCampo('cpf')}
              </div>

              <div className="cadrec-grupo">
                <label htmlFor="cartaoSus">Cartão SUS</label>
                <input
                  id="cartaoSus"
                  className={classeCampo('cartaoSus')}
                  type="text"
                  inputMode="numeric"
                  placeholder="700 1234 5678 9012"
                  autoComplete="off"
                  value={dados.cartaoSus}
                  onChange={(e) => alterar('cartaoSus', mascararCartaoSus(e.target.value))}
                  {...acessibilidade('cartaoSus')}
                />
                {erroDoCampo('cartaoSus')}
              </div>
            </div>
          </section>

          <section className="cadrec-secao">
            <h2 className="cadrec-secao-titulo">
              <span className="cadrec-numero" aria-hidden="true">2</span>
              Contato e prioridade
            </h2>

            <div className="cadrec-linha">
              <div className="cadrec-grupo">
                <label htmlFor="telefone">Telefone (opcional)</label>
                <input
                  id="telefone"
                  className={classeCampo('telefone')}
                  type="tel"
                  placeholder="(83) 3333-0000"
                  autoComplete="off"
                  value={dados.telefone}
                  onChange={(e) => alterar('telefone', mascararTelefone(e.target.value))}
                  {...acessibilidade('telefone')}
                />
                {erroDoCampo('telefone')}
              </div>

              <div className="cadrec-grupo">
                <label htmlFor="prioridade">Necessidade de prioridade</label>
                <select
                  id="prioridade"
                  className="cadrec-campo cadrec-select"
                  value={dados.prioridade}
                  onChange={(e) => alterar('prioridade', e.target.value as PrioridadeRecepcao)}
                >
                  {OPCOES_PRIORIDADE.map((opcao) => (
                    <option key={opcao.valor} value={opcao.valor}>
                      {opcao.texto}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </section>

          <section className="cadrec-secao">
            <h2 className="cadrec-secao-titulo">
              <span className="cadrec-numero" aria-hidden="true">3</span>
              Consentimento
            </h2>

            <div className={classeConsentimento}>
              <p className="cadrec-termo-titulo">Termo de consentimento (LGPD)</p>
              <p className="cadrec-termo-texto">
                Leia ou explique ao paciente: os dados (CPF, cartão SUS e contato) serão usados apenas para
                organizar o atendimento nesta UBS, e ele pode pedir a exclusão a qualquer momento.
              </p>
              <label className="cadrec-consentimento">
                <input
                  type="checkbox"
                  checked={dados.consentimentoVerbal}
                  onChange={(e) => alterar('consentimentoVerbal', e.target.checked)}
                  {...acessibilidade('consentimentoVerbal')}
                />
                <span>Paciente leu (ou ouviu) o termo e deu consentimento verbal</span>
              </label>
            </div>
            {erroDoCampo('consentimentoVerbal')}
          </section>

          <div className="cadrec-rodape">
            <p>CPF e cartão SUS são guardados protegidos.</p>
            <button type="submit" className="cadrec-botao" disabled={enviando}>
              {enviando && <span className="cadrec-carregando" aria-hidden="true" />}
              {textoBotao}
            </button>
          </div>
        </form>

        <aside className="cadrec-cartao cadrec-lado" aria-live="polite">
          {resultado !== null && !resultado.jaCadastrado && (
            <div className="cadrec-estado" role="status">
              <span className="cadrec-estado-icone cadrec-estado-sucesso" aria-hidden="true">
                <IconeCheck />
              </span>
              <h2>Paciente cadastrado</h2>
              <p className="cadrec-detalhe">Já pode entrar na fila.</p>
              <dl className="cadrec-ficha">
                <dt>Nome</dt>
                <dd>{resultado.nome}</dd>
                <dt>CPF</dt>
                <dd>{resultado.cpfMascarado}</dd>
                <dt>Prioridade</dt>
                <dd>{textoPrioridade(resultado.prioridade)}</dd>
              </dl>
              {resultado.consentimentoEm !== null && (
                <p className="cadrec-registro">
                  Consentimento verbal registrado em {formatarDataHora(resultado.consentimentoEm)} (termo
                  versão {resultado.versaoTermo})
                </p>
              )}
            </div>
          )}

          {resultado !== null && resultado.jaCadastrado && (
            <div className="cadrec-estado" role="status">
              <span className="cadrec-estado-icone cadrec-estado-info" aria-hidden="true">
                <IconePessoa />
              </span>
              <h2>CPF já cadastrado</h2>
              <p className="cadrec-detalhe">Abrimos o cadastro existente em vez de criar outro.</p>
              <div className="cadrec-encontrado">
                <h3>Paciente encontrado</h3>
                <p>
                  {resultado.nome} · CPF {resultado.cpfMascarado} · cadastrado em{' '}
                  {formatarData(resultado.cadastradoEm)}
                </p>
                <p>Prioridade: {textoPrioridade(resultado.prioridade)}</p>
              </div>
            </div>
          )}

          {resultado === null && (
            <div className="cadrec-estado">
              <span className="cadrec-estado-icone cadrec-estado-neutro" aria-hidden="true">
                <IconeBusca />
              </span>
              <h2>CPF já cadastrado?</h2>
              <p className="cadrec-detalhe">
                Se o CPF já existir, o FilaSUS abre o cadastro existente em vez de criar outro.
              </p>
            </div>
          )}
        </aside>
      </div>
    </div>
  )
}
