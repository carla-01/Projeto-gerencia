import type { ReactNode } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { fazerLogout } from '../services/auth'
import './LayoutRecepcao.css'

type Props = {
  children: ReactNode
}

/** Cruz da saúde usada como logo do FilaSUS. */
function IconeCruz() {
  return (
    <svg className="recepcao-logo-icone" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M9 2h6v7h7v6h-7v7H9v-7H2V9h7z" fill="currentColor" />
    </svg>
  )
}

/** Ícone de lista, para a fila do dia. */
function IconeFila() {
  return (
    <svg className="recepcao-menu-icone" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M8 6h12M8 12h12M8 18h12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <circle cx="4" cy="6" r="1.3" fill="currentColor" />
      <circle cx="4" cy="12" r="1.3" fill="currentColor" />
      <circle cx="4" cy="18" r="1.3" fill="currentColor" />
    </svg>
  )
}

/** Ícone de pessoa com "+", para o cadastro. */
function IconeCadastrar() {
  return (
    <svg className="recepcao-menu-icone" viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="9" cy="8" r="3.5" fill="none" stroke="currentColor" strokeWidth="1.9" />
      <path d="M2.5 20c0-3.6 2.9-5.5 6.5-5.5s6.5 1.9 6.5 5.5" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
      <path d="M19 8v6M16 11h6" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
    </svg>
  )
}

/**
 * Moldura das telas da recepção, conforme os protótipos R2 e R3:
 * barra lateral azul com o menu no computador; no celular, barra no topo.
 */
export default function LayoutRecepcao({ children }: Props) {
  const navigate = useNavigate()

  function sair() {
    fazerLogout()
    navigate('/login', { replace: true })
  }

  // O item do menu da página aberta fica destacado.
  function classeItem({ isActive }: { isActive: boolean }) {
    if (isActive) {
      return 'recepcao-menu-item recepcao-menu-item-ativo'
    }
    return 'recepcao-menu-item'
  }

  return (
    <div className="recepcao-pagina">
      <aside className="recepcao-lateral">
        <div className="recepcao-logo">
          <span className="recepcao-logo-selo">
            <IconeCruz />
          </span>
          <div>
            <strong>FilaSUS</strong>
            <span>Unidade Básica de Saúde</span>
          </div>
        </div>

        <nav className="recepcao-menu" aria-label="Menu da recepção">
          <NavLink to="/recepcao" end className={classeItem}>
            <IconeFila />
            <span>Fila do dia</span>
          </NavLink>
          <NavLink to="/recepcao/cadastrar" className={classeItem}>
            <IconeCadastrar />
            <span>Cadastrar paciente</span>
          </NavLink>
        </nav>

        <div className="recepcao-usuario">
          <span className="recepcao-avatar" aria-hidden="true">R</span>
          <div className="recepcao-usuario-texto">
            <strong>Recepção</strong>
            <button type="button" className="recepcao-sair" onClick={sair}>
              Sair
            </button>
          </div>
        </div>
      </aside>

      <div className="recepcao-conteudo">{children}</div>
    </div>
  )
}
