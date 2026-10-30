import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { estaAutenticado, obterPerfil } from '../services/auth'
import type { Perfil } from '../services/auth'

type Props = {
  perfis: Perfil[]
  children: ReactNode
}

export default function ProtectedRoute({ perfis, children }: Props) {
  const location = useLocation()
  const perfil = obterPerfil()

  if (!estaAutenticado()) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (perfil === null || !perfis.includes(perfil)) {
    return <Navigate to="/acesso-negado" replace />
  }

  return children
}
