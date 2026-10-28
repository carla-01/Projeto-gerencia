import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fazerLogout, obterToken } from '../services/auth'
import './PainelRecepcao.css'

type RespostaFila = {
  usuarioId: string
  fila: unknown[]
}

export default function PainelRecepcao() {
  const navigate = useNavigate()
  const [fila, setFila] = useState<unknown[]>([])
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    const controller = new AbortController()

    async function carregarFila() {
      const token = obterToken()
      const resposta = await fetch('/api/recepcao/fila', {
        headers: { Authorization: `Bearer ${token}` },
        signal: controller.signal,
      })

      if (resposta.status === 403) {
        navigate('/acesso-negado', { replace: true })
        return
      }

      if (resposta.status === 401) {
        fazerLogout()
        navigate('/login', { replace: true })
        return
      }

      if (!resposta.ok) {
        throw new Error('Não foi possível carregar a fila.')
      }

      const dados: RespostaFila = await resposta.json()
      setFila(dados.fila)
      setCarregando(false)
    }

    carregarFila().catch(() => setCarregando(false))
    return () => controller.abort()
  }, [navigate])

  if (carregando) {
    return <main className="painel-recepcao"><p>Carregando fila do dia...</p></main>
  }

  return (
    <main className="painel-recepcao">
      <h1>Painel da recepção</h1>
      <h2>Fila do dia</h2>
      {fila.length === 0 ? (
        <p>Nenhuma pessoa na fila.</p>
      ) : (
        <ul>{fila.map((item, indice) => <li key={indice}>{String(item)}</li>)}</ul>
      )}
    </main>
  )
}
