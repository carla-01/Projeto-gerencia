import { Link, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import Cadastro from './pages/Cadastro'
import Login from './pages/Login'
import Pagina from './pages/Pagina'
import ProtectedRoute from './components/ProtectedRoute'
import AcessoNegado from './pages/AcessoNegado'
import CadastroRecepcao from './pages/CadastroRecepcao'
import LayoutRecepcao from './components/LayoutRecepcao'
import PainelRecepcao from './pages/PainelRecepcao'
import LayoutProfissional from './components/LayoutProfissional'
import PainelProfissional from './pages/PainelProfissional'
import ConfigurarFilas from './pages/ConfigurarFilas'
import Funcionarios from './pages/Funcionarios'
import LayoutGestor from './components/LayoutGestor'
import EscolhaServico from './pages/EscolhaServico'
import { obterPerfil } from './services/auth'

/**
 * Rotas por perfil, ainda vazias.
 * - US01 (SCRUM-37): tela de login.
 * - US02 (SCRUM-38): bloqueia as rotas que o perfil logado não pode abrir.
 * - US04 (SCRUM-39): tela de autocadastro do paciente.
 * - US05 (SCRUM-40): cadastro de paciente pela recepção.
 * - US06 (SCRUM-137): o paciente escolhe o serviço e entra na fila.
 * - US03 (SCRUM-152): área do gestor com o cadastro e a lista de funcionários.
 * - US13 (SCRUM-164): o profissional chama o próximo paciente.
 */
export default function App() {
  const location = useLocation()
  const perfil = obterPerfil()

  // As telas de login e de cadastro ocupam a página inteira, sem o menu provisório do esqueleto.
  // As telas da recepção, do gestor e do profissional têm o próprio menu (barra lateral).
  let mostrarMenu = true
  if (location.pathname === '/login' || location.pathname === '/cadastro') {
    mostrarMenu = false
  }
  if (location.pathname.startsWith('/recepcao') || location.pathname.startsWith('/gestor')) {
    mostrarMenu = false
  }
  if (location.pathname === '/profissional') {
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
            {perfil === 'GESTOR' && <Link to="/gestor/configurar-filas">Configurar filas</Link>}
          </main>
        </nav>
      )}
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/cadastro" element={<Cadastro />} />
        <Route path="/paciente" element={
          <ProtectedRoute perfis={['PACIENTE']}>
            <EscolhaServico />
          </ProtectedRoute>
        } />
        <Route path="/recepcao" element={
          <ProtectedRoute perfis={['RECEPCIONISTA']}>
            <LayoutRecepcao><PainelRecepcao /></LayoutRecepcao>
          </ProtectedRoute>
        } />
        <Route path="/recepcao/cadastrar" element={
          <ProtectedRoute perfis={['RECEPCIONISTA']}>
            <LayoutRecepcao><CadastroRecepcao /></LayoutRecepcao>
          </ProtectedRoute>
        } />
        <Route path="/acesso-negado" element={<AcessoNegado />} />
        <Route path="/profissional" element={
          <ProtectedRoute perfis={['PROFISSIONAL']}>
            <LayoutProfissional><PainelProfissional /></LayoutProfissional>
          </ProtectedRoute>
        } />
        <Route path="/gestor" element={
          <ProtectedRoute perfis={['GESTOR']}>
            <LayoutGestor><Funcionarios /></LayoutGestor>
          </ProtectedRoute>
        } />
        <Route path="/gestor/configurar-filas" element={
          <ProtectedRoute perfis={['GESTOR']}>
            <LayoutGestor><ConfigurarFilas /></LayoutGestor>
          </ProtectedRoute>
        } />
        <Route path="*" element={<Pagina titulo="Página não encontrada" descricao="Confira o endereço." />} />
      </Routes>
    </>
  )
}
