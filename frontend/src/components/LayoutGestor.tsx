import type { ReactNode } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { fazerLogout } from '../services/auth'
// Mesma moldura visual da recepção (protótipos R2 e R3): barra lateral azul no computador, topo no celular.
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

/** Ícone de grupo de pessoas, para os funcionários. */
function IconeFuncionarios() {
  return (
    <svg className="recepcao-menu-icone" viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="9" cy="8" r="3.5" fill="none" stroke="currentColor" strokeWidth="1.9" />
      <path d="M2.5 20c0-3.6 2.9-5.5 6.5-5.5s6.5 1.9 6.5 5.5" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
      <path d="M16 4.8a3.5 3.5 0 0 1 0 6.4M18.5 14.8c1.9.7 3 2.4 3 5.2" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
    </svg>
  )
}

/** Ícone de relógio, para os horários das filas. */
function IconeFilas() {
  return (
    <svg className="recepcao-menu-icone" viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="8.5" fill="none" stroke="currentColor" strokeWidth="1.9" />
      <path d="M12 7.5V12l3 2" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
    </svg>
  )
}

/** Moldura das telas do gestor (US03 - SCRUM-152), com o menu do gestor. */
export default function LayoutGestor({ children }: Props) {
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

        <nav className="recepcao-menu" aria-label="Menu do gestor">
          <NavLink to="/gestor" end className={classeItem}>
            <IconeFuncionarios />
            <span>Funcionários</span>
          </NavLink>
          <NavLink to="/gestor/configurar-filas" className={classeItem}>
            <IconeFilas />
            <span>Configurar filas</span>
          </NavLink>
        </nav>

        <div className="recepcao-usuario">
          <span className="recepcao-avatar" aria-hidden="true">G</span>
          <div className="recepcao-usuario-texto">
            <strong>Gestão</strong>
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
