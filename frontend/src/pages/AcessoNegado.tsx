import { useNavigate } from 'react-router-dom'
import './AcessoNegado.css'

export default function AcessoNegado() {
  const navigate = useNavigate()

  return (
    <main className="acesso-negado">
      <h1>Acesso não permitido</h1>
      <button type="button" onClick={() => navigate(-1)}>
        Voltar
      </button>
    </main>
  )
}
