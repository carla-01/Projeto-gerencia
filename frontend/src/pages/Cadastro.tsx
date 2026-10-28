import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { autocadastrar, ErroAutocadastro } from '../services/autocadastro'
import type { CadastroConcluido, CampoDoCadastro, DadosAutocadastro, Prioridade } from '../services/autocadastro'
import { cpfValido, mascararCpf, somenteNumeros } from '../services/cpf'
import './Cadastro.css'

/** Mesma frase que o back-end devolve quando falta o aceite (CT-US04-02). */
const FINALIDADE_DOS_DADOS =
  'Para se cadastrar, aceite o termo. Seus dados são usados só para organizar ' +
  'a fila de atendimento da UBS, e você pode pedir a exclusão deles quando quiser.'

const OPCOES_PRIORIDADE: { valor: Prioridade; texto: string }[] = [
  { valor: 'NENHUMA', texto: 'Não tenho prioridade' },
  { valor: 'IDOSO', texto: 'Idoso (60 anos ou mais)' },
  { valor: 'GESTANTE', texto: 'Gestante' },
  { valor: 'PCD', texto: 'Pessoa com deficiência' },
]

const DADOS_VAZIOS: DadosAutocadastro = {
  nome: '',
  cpf: '',
  cartaoSus: '',
  telefone: '',
  email: '',
  senha: '',
  prioridade: 'NENHUMA',
  aceiteTermo: false,
}

type ErrosDosCampos = Partial<Record<CampoDoCadastro, string>>

/** Coloca a máscara (83) 99999-0000 enquanto a pessoa digita. */
function mascararTelefone(texto: string): string {
  const numeros = somenteNumeros(texto).slice(0, 11)
  if (numeros.length <= 2) {
    return numeros
  }
  if (numeros.length <= 6) {
    return `(${numeros.slice(0, 2)}) ${numeros.slice(2)}`
  }
  const meio = numeros.length === 11 ? 7 : 6
  return `(${numeros.slice(0, 2)}) ${numeros.slice(2, meio)}-${numeros.slice(meio)}`
}

/** Confere os campos antes de enviar. Devolve só os campos com erro. */
function validar(dados: DadosAutocadastro): ErrosDosCampos {
  const erros: ErrosDosCampos = {}

  if (dados.nome.trim() === '') {
    erros.nome = 'Informe seu nome.'
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
  if (!/^\S+@\S+\.\S+$/.test(dados.email.trim())) {
    erros.email = 'Informe um e-mail válido.'
  }
  if (dados.senha.length < 8) {
    erros.senha = 'A senha precisa ter pelo menos 8 caracteres.'
  }
  if (!dados.aceiteTermo) {
    erros.aceiteTermo = FINALIDADE_DOS_DADOS
  }

  return erros
}

/** "2026-10-28T20:24:26Z" vira "28/10/2026, 17:24" no horário do navegador. */
function formatarDataHora(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

/**
 * Autocadastro do paciente pelo celular (US04 - SCRUM-39).
 *
 * O paciente preenche os dados, lê o termo LGPD e marca o aceite.
 * Sem o aceite o cadastro não é concluído e a finalidade dos dados aparece
 * (CT-US04-02). Com CPF errado aparece "CPF inválido" no campo (CT-US04-03).
 */
export default function Cadastro() {
  const [dados, setDados] = useState<DadosAutocadastro>(DADOS_VAZIOS)
  const [erros, setErros] = useState<ErrosDosCampos>({})
  const [erroEnvio, setErroEnvio] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [concluido, setConcluido] = useState<CadastroConcluido | null>(null)

  /** Atualiza um campo e apaga o erro dele, se houver. */
  function alterar<Campo extends CampoDoCadastro>(campo: Campo, valor: DadosAutocadastro[Campo]) {
    setDados((atual) => ({ ...atual, [campo]: valor }))
    setErros((atual) => ({ ...atual, [campo]: undefined }))
  }

  async function cadastrar(evento: FormEvent) {
    // Impede o navegador de recarregar a página ao enviar o formulário.
    evento.preventDefault()
    setErroEnvio('')

    const errosEncontrados = validar(dados)
    setErros(errosEncontrados)
    if (Object.keys(errosEncontrados).length > 0) {
      return
    }

    setEnviando(true)
    try {
      setConcluido(await autocadastrar(dados))
    } catch (e) {
      if (e instanceof ErroAutocadastro && e.campo !== null) {
        // O back-end disse qual campo está errado: o erro aparece embaixo dele.
        const campo = e.campo
        setErros((atual) => ({ ...atual, [campo]: e.message }))
      } else if (e instanceof ErroAutocadastro) {
        setErroEnvio(e.message)
      } else {
        setErroEnvio('Não foi possível concluir o cadastro agora. Tente de novo.')
      }
    }
    setEnviando(false)
  }

  /** Classe do campo: com borda vermelha quando tem erro. */
  function classeCampo(campo: CampoDoCadastro): string {
    if (erros[campo]) {
      return 'cadastro-campo cadastro-campo-erro'
    }
    return 'cadastro-campo'
  }

  /** Mensagem de erro embaixo do campo, ligada a ele para leitores de tela. */
  function erroDoCampo(campo: CampoDoCadastro) {
    if (!erros[campo]) {
      return null
    }
    return (
      <p id={`erro-${campo}`} className="cadastro-erro-campo">
        {erros[campo]}
      </p>
    )
  }

  /** Liga o campo à mensagem de erro dele. */
  function acessibilidade(campo: CampoDoCadastro) {
    if (!erros[campo]) {
      return {}
    }
    return { 'aria-invalid': true, 'aria-describedby': `erro-${campo}` }
  }

  return (
    <div className="cadastro-pagina">
      <header className="cadastro-topo">
        <span className="cadastro-logo-selo" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M9 2h6v7h7v6h-7v7H9v-7H2V9h7z" fill="currentColor" />
          </svg>
        </span>
        <span>FilaSUS</span>
      </header>

      <main className="cadastro-conteudo">
        {concluido !== null ? (
          <section className="cadastro-cartao cadastro-sucesso" role="status">
            <span className="cadastro-sucesso-icone" aria-hidden="true">✓</span>
            <h1>Conta criada</h1>
            <p>
              Pronto, {concluido.nome}! Agora é só entrar com seu CPF ou e-mail e a senha que você criou.
            </p>
            <p className="cadastro-aceite-registrado">
              Aceite do termo (versão {concluido.versaoTermo}) registrado em {formatarDataHora(concluido.aceitoEm)}.
            </p>
            <Link to="/login" className="cadastro-botao">
              Entrar
            </Link>
          </section>
        ) : (
          <form className="cadastro-cartao" onSubmit={cadastrar} noValidate>
            <h1>Criar conta</h1>
            <p className="cadastro-descricao">
              Cadastre-se para pegar sua senha sem enfrentar fila. Todos os campos são obrigatórios,
              menos o telefone.
            </p>

            {erroEnvio !== '' && (
              <div className="cadastro-erro" role="alert">
                {erroEnvio}
              </div>
            )}

            <label htmlFor="nome">Nome completo</label>
            <input
              id="nome"
              className={classeCampo('nome')}
              type="text"
              autoComplete="name"
              maxLength={120}
              value={dados.nome}
              onChange={(e) => alterar('nome', e.target.value)}
              {...acessibilidade('nome')}
            />
            {erroDoCampo('nome')}

            <div className="cadastro-linha">
              <div>
                <label htmlFor="cpf">CPF</label>
                <input
                  id="cpf"
                  className={classeCampo('cpf')}
                  type="text"
                  inputMode="numeric"
                  autoComplete="off"
                  placeholder="000.000.000-00"
                  value={dados.cpf}
                  onChange={(e) => alterar('cpf', mascararCpf(e.target.value))}
                  {...acessibilidade('cpf')}
                />
                {erroDoCampo('cpf')}
              </div>

              <div>
                <label htmlFor="cartaoSus">Cartão SUS</label>
                <input
                  id="cartaoSus"
                  className={classeCampo('cartaoSus')}
                  type="text"
                  inputMode="numeric"
                  autoComplete="off"
                  placeholder="15 números"
                  value={dados.cartaoSus}
                  onChange={(e) => alterar('cartaoSus', somenteNumeros(e.target.value).slice(0, 15))}
                  {...acessibilidade('cartaoSus')}
                />
                {erroDoCampo('cartaoSus')}
              </div>
            </div>

            <div className="cadastro-linha">
              <div>
                <label htmlFor="email">E-mail</label>
                <input
                  id="email"
                  className={classeCampo('email')}
                  type="email"
                  autoComplete="email"
                  maxLength={160}
                  value={dados.email}
                  onChange={(e) => alterar('email', e.target.value)}
                  {...acessibilidade('email')}
                />
                {erroDoCampo('email')}
              </div>

              <div>
                <label htmlFor="telefone">Telefone (opcional)</label>
                <input
                  id="telefone"
                  className={classeCampo('telefone')}
                  type="tel"
                  autoComplete="tel"
                  placeholder="(83) 99999-0000"
                  value={dados.telefone}
                  onChange={(e) => alterar('telefone', mascararTelefone(e.target.value))}
                  {...acessibilidade('telefone')}
                />
                {erroDoCampo('telefone')}
              </div>
            </div>

            <label htmlFor="senha">Senha</label>
            <input
              id="senha"
              className={classeCampo('senha')}
              type="password"
              autoComplete="new-password"
              placeholder="Pelo menos 8 caracteres"
              maxLength={72}
              value={dados.senha}
              onChange={(e) => alterar('senha', e.target.value)}
              {...acessibilidade('senha')}
            />
            {erroDoCampo('senha')}

            <label htmlFor="prioridade">Prioridade legal</label>
            <select
              id="prioridade"
              className="cadastro-campo"
              value={dados.prioridade}
              onChange={(e) => alterar('prioridade', e.target.value as Prioridade)}
            >
              {OPCOES_PRIORIDADE.map((opcao) => (
                <option key={opcao.valor} value={opcao.valor}>
                  {opcao.texto}
                </option>
              ))}
            </select>

            <fieldset className={erros.aceiteTermo ? 'cadastro-termo cadastro-termo-erro' : 'cadastro-termo'}>
              <legend>Termo de consentimento (LGPD)</legend>
              <p>
                Seus dados (nome, CPF, cartão SUS e contato) são usados só para organizar a fila de
                atendimento da UBS e avisar quando sua vez estiver chegando. O CPF fica protegido e
                não é compartilhado. Você pode pedir a exclusão dos seus dados quando quiser.
              </p>
              <label className="cadastro-termo-aceite">
                <input
                  type="checkbox"
                  checked={dados.aceiteTermo}
                  onChange={(e) => alterar('aceiteTermo', e.target.checked)}
                  {...acessibilidade('aceiteTermo')}
                />
                Li e aceito o termo de consentimento
              </label>
              {erroDoCampo('aceiteTermo')}
            </fieldset>

            <button type="submit" className="cadastro-botao" disabled={enviando}>
              {enviando && <span className="cadastro-carregando" aria-hidden="true" />}
              {enviando ? 'Criando conta...' : 'Criar conta'}
            </button>

            <p className="cadastro-rodape">
              Já tem conta? <Link to="/login">Entrar</Link>
            </p>
          </form>
        )}
      </main>
    </div>
  )
}
