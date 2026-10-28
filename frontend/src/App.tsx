import { Link, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import Cadastro from './pages/Cadastro'
import Login from './pages/Login'
import Pagina from './pages/Pagina'
import ProtectedRoute from './components/ProtectedRoute'
import AcessoNegado from './pages/AcessoNegado'
import PainelRecepcao from './pages/PainelRecepcao'
import { obterPerfil } from './services/auth'

/**
 * Rotas por perfil, ainda vazias.
 * - US01 (SCRUM-37): tela de login.
 * - US02 (SCRUM-38): bloqueia as rotas que o perfil logado não pode abrir.
 * - US04 (SCRUM-39): tela de autocadastro do paciente.
 * - US05 (SCRUM-40): cadastro de paciente pela recepção.
 */
export default function App() {
  const location = useLocation()
  const perfil = obterPerfil()

  // As telas de login e de cadastro ocupam a página inteira, sem o menu provisório do esqueleto.
  let mostrarMenu = true
  if (location.pathname === '/login' || location.pathname === '/cadastro') {
    mostrarMenu = false
  }

  return (
    <>
      {mostrarMenu && (
        <nav>
          <main>
            <Link to="/login">Login</Link>
            <Link to="/paciente">Paciente</Link>
            {perfil === 'RECEPCIONISTA' && <Link to="/recepcao">Recepção</Link>}
            <Link to="/profissional">Profissional</Link>
            <Link to="/gestor">Gestor</Link>
          </main>
        </nav>
      )}
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/cadastro" element={<Cadastro />} />
        <Route path="/paciente" element={<Pagina titulo="Área do paciente" descricao="Fila e posição do paciente (R2)." />} />
        <Route path="/recepcao" element={
          <ProtectedRoute perfis={['RECEPCIONISTA']}><PainelRecepcao /></ProtectedRoute>
        } />
        <Route path="/acesso-negado" element={<AcessoNegado />} />
        <Route path="/profissional" element={<Pagina titulo="Profissional de saúde" descricao="Chamada e atendimento (R2)." />} />
        <Route path="/gestor" element={<Pagina titulo="Gestor" descricao="Configuração e relatórios (R2 a R4)." />} />
        <Route path="*" element={<Pagina titulo="Página não encontrada" descricao="Confira o endereço." />} />
      </Routes>
    </>
  )
}
