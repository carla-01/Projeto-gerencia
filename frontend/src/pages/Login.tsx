import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ErroLogin, fazerLogin, telaInicialDoPerfil } from '../services/auth'
import './Login.css'

/** Cruz da saúde usada como logo do FilaSUS. */
function IconeCruz({ className }: { className: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true">
      <path d="M9 2h6v7h7v6h-7v7H9v-7H2V9h7z" fill="currentColor" />
    </svg>
  )
}

/** Ícone de pessoa, dentro do campo CPF ou e-mail. */
function IconeUsuario() {
  return (
    <svg className="login-campo-icone" viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="8" r="4" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path d="M4 20c0-4 3.6-6 8-6s8 2 8 6" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

/** Ícone de cadeado, dentro do campo Senha. */
function IconeCadeado() {
  return (
    <svg className="login-campo-icone" viewBox="0 0 24 24" aria-hidden="true">
      <rect x="5" y="10" width="14" height="10" rx="2" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path d="M8 10V7a4 4 0 0 1 8 0v3" fill="none" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  )
}

/** Ícone de alerta, na caixa de erro. */
function IconeAlerta() {
  return (
    <svg className="login-erro-icone" viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path d="M12 7.5v5.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <circle cx="12" cy="16.5" r="1.2" fill="currentColor" />
    </svg>
  )
}

/**
 * Tela de login (US01 - SCRUM-37), conforme o protótipo P1.
 * Depois do login, leva o usuário para a tela inicial do perfil dele.
 *
 * - No celular: barra azul no topo e o formulário ocupando a tela.
 * - No computador: painel azul do FilaSUS à esquerda e o formulário à direita.
 */
export default function Login() {
  const navigate = useNavigate()
  const [login, setLogin] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function entrar(evento: FormEvent) {
    // Impede o navegador de recarregar a página ao enviar o formulário.
    evento.preventDefault()
    setErro('')
    setEnviando(true)

    try {
      const perfil = await fazerLogin(login, senha)
      navigate(telaInicialDoPerfil(perfil), { replace: true })
    } catch (e) {
      if (e instanceof ErroLogin) {
        setErro(e.message)
      } else {
        setErro('Não foi possível entrar agora. Tente de novo.')
      }
      setEnviando(false)
    }
  }

  let textoBotao = 'Entrar'
  if (enviando) {
    textoBotao = 'Entrando...'
  }

  // Com erro, os campos ficam com a borda vermelha (protótipo P1b).
  let classeCampo = 'login-campo'
  if (erro !== '') {
    classeCampo = 'login-campo login-campo-erro'
  }

  return (
    <div className="login-pagina">
      {/* Painel azul: no celular vira a barra do topo */}
      <aside className="login-marca">
        <div className="login-logo">
          <span className="login-logo-selo">
            <IconeCruz className="login-logo-icone" />
          </span>
          <span>FilaSUS</span>
        </div>

        <div className="login-marca-texto">
          <p className="login-marca-rotulo">Unidade Básica de Saúde</p>
          <h2>Atendimento organizado, sem fila na porta.</h2>
          <p>Pegue sua senha e acompanhe sua vez de onde estiver.</p>
        </div>

        <IconeCruz className="login-marca-fundo" />
      </aside>

      <main className="login-conteudo">
        <form className="login-form" onSubmit={entrar} noValidate>
          <h1>Bem-vindo ao FilaSUS</h1>
          <p className="login-descricao">
            Entre com seu CPF ou e-mail para pegar sua senha sem enfrentar fila.
          </p>

          {erro !== '' && (
            <div className="login-erro" role="alert">
              <IconeAlerta />
              <div>
                <strong>{erro}</strong>
                <span>Confira os dados digitados e tente de novo.</span>
              </div>
            </div>
          )}

          <label htmlFor="login">CPF ou e-mail</label>
          <div className="login-campo-grupo">
            <IconeUsuario />
            <input
              id="login"
              className={classeCampo}
              type="text"
              autoComplete="username"
              placeholder="000.000.000-00"
              value={login}
              onChange={(e) => setLogin(e.target.value)}
              required
            />
          </div>

          <label htmlFor="senha">Senha</label>
          <div className="login-campo-grupo">
            <IconeCadeado />
            <input
              id="senha"
              className={classeCampo}
              type="password"
              autoComplete="current-password"
              placeholder="Digite sua senha"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              required
            />
          </div>

          <button type="submit" className="login-botao" disabled={enviando}>
            {enviando && <span className="login-carregando" aria-hidden="true" />}
            {textoBotao}
          </button>
        </form>
      </main>
    </div>
  )
}
