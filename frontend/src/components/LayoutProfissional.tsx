import type { ReactNode } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { fazerLogout } from '../services/auth'
// Mesma moldura das telas da recepção e do gestor.
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

/** Ícone de megafone, para a chamada. */
function IconeChamada() {
  return (
    <svg className="recepcao-menu-icone" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M3 10v4h4l8 5V5l-8 5H3z" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinejoin="round" />
      <path d="M18.5 9a4 4 0 010 6" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
    </svg>
  )
}

/** Moldura da tela do profissional de saúde: barra lateral no computador e no topo no celular. */
export default function LayoutProfissional({ children }: Props) {
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

        <nav className="recepcao-menu" aria-label="Menu do profissional">
          <NavLink to="/profissional" end className={classeItem}>
            <IconeChamada />
            <span>Atendimento</span>
          </NavLink>
        </nav>

        <div className="recepcao-usuario">
          <span className="recepcao-avatar" aria-hidden="true">P</span>
          <div className="recepcao-usuario-texto">
            <strong>Profissional</strong>
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
