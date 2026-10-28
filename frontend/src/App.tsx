import { Link, Navigate, Route, Routes } from 'react-router-dom'
import Pagina from './pages/Pagina'

/**
 * Rotas por perfil, ainda vazias.
 * - US01 (SCRUM-37): tela de login.
 * - US02 (SCRUM-38): bloqueia as rotas que o perfil logado não pode abrir.
 * - US04 (SCRUM-39): tela de autocadastro do paciente.
 * - US05 (SCRUM-40): cadastro de paciente pela recepção.
 */
export default function App() {
  return (
    <>
      <nav>
        <main>
          <Link to="/login">Login</Link>
          <Link to="/paciente">Paciente</Link>
          <Link to="/recepcao">Recepção</Link>
          <Link to="/profissional">Profissional</Link>
          <Link to="/gestor">Gestor</Link>
        </main>
      </nav>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<Pagina titulo="Entrar" descricao="Tela de login (US01)." />} />
        <Route path="/cadastro" element={<Pagina titulo="Cadastro do paciente" descricao="Autocadastro com consentimento LGPD (US04)." />} />
        <Route path="/paciente" element={<Pagina titulo="Área do paciente" descricao="Fila e posição do paciente (R2)." />} />
        <Route path="/recepcao" element={<Pagina titulo="Recepção" descricao="Cadastro de paciente pela recepção (US05) e painel da fila." />} />
        <Route path="/profissional" element={<Pagina titulo="Profissional de saúde" descricao="Chamada e atendimento (R2)." />} />
        <Route path="/gestor" element={<Pagina titulo="Gestor" descricao="Configuração e relatórios (R2 a R4)." />} />
        <Route path="*" element={<Pagina titulo="Página não encontrada" descricao="Confira o endereço." />} />
      </Routes>
    </>
  )
}
