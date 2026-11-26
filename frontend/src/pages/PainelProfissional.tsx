import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fazerLogout } from '../services/auth'
import { ErroChamada, chamarProximo } from '../services/chamada'
import type { SenhaChamada } from '../services/chamada'
import type { PrioridadeSenha } from '../services/checkIn'
import './PainelProfissional.css'

/** 7 vira "007", como no painel de senhas. */
function formatarNumero(numero: number): string {
  return String(numero).padStart(3, '0')
}

/** "2026-11-25T12:00:00Z" vira "09:00" no horário do navegador. */
function formatarHora(iso: string): string {
  return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

function textoPrioridade(prioridade: PrioridadeSenha): string {
  if (prioridade === 'IDOSO') {
    return 'Idoso 60+'
  }
  if (prioridade === 'GESTANTE') {
    return 'Gestante'
  }
  if (prioridade === 'PCD') {
    return 'Pessoa com deficiência'
  }
  if (prioridade === 'URGENCIA') {
    return 'Urgência'
  }
  return ''
}

function IconeChamada() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M3 10v4h4l8 5V5l-8 5H3z" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinejoin="round" />
      <path d="M18.5 9a4 4 0 010 6" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
    </svg>
  )
}

/**
 * Tela do profissional de saúde (US13 - SCRUM-164).
 *
 * "Chamar próximo" chama quem fez check-in no setor dele, pela ordem de prioridade (CT-US13-01).
 * Sem ninguém presente, aparece "Não há pacientes aguardando" (CT-US13-02).
 */
export default function PainelProfissional() {
  const navigate = useNavigate()
  const [senha, setSenha] = useState<SenhaChamada | null>(null)
  const [aviso, setAviso] = useState('')
  const [erro, setErro] = useState('')
  const [chamando, setChamando] = useState(false)

  function tratarErro(e: unknown) {
    if (!(e instanceof ErroChamada)) {
      setErro('Algo deu errado. Tente de novo.')
      return
    }
    if (e.tipo === 'sem-login') {
      fazerLogout()
      navigate('/login', { replace: true })
      return
    }
    if (e.tipo === 'sem-permissao') {
      navigate('/acesso-negado', { replace: true })
      return
    }
    setErro(e.message)
  }

  async function chamar() {
    setErro('')
    setAviso('')
    setChamando(true)
    try {
      const resultado = await chamarProximo()
      if (resultado.senha !== null) {
        setSenha(resultado.senha)
      }
      // Com a fila vazia, o último chamado continua na tela.
      if (resultado.mensagem !== null) {
        setAviso(resultado.mensagem)
      }
    } catch (e) {
      tratarErro(e)
    }
    setChamando(false)
  }

  let textoBotao = 'Chamar próximo'
  if (chamando) {
    textoBotao = 'Chamando...'
  }

  return (
    <main className="profissional">
      <header className="profissional-cabecalho">
        <p>Atendimento</p>
        <h1>Profissional de saúde</h1>
      </header>

      <section className="profissional-cartao">
        <div className="profissional-topo">
          <div>
            <h2>Fila do meu setor</h2>
            <p>Chama quem já fez check-in, respeitando a prioridade legal.</p>
          </div>
          <button type="button" className="profissional-chamar" disabled={chamando} onClick={chamar}>
            <IconeChamada />
            {textoBotao}
          </button>
        </div>

        <div className="profissional-corpo" aria-live="polite">
          {erro !== '' && (
            <p className="profissional-erro" role="alert">
              {erro}
            </p>
          )}
          {aviso !== '' && (
            <p className="profissional-aviso" role="status">
              {aviso}
            </p>
          )}

          {senha === null && (
            <p className="profissional-vazio">Nenhum paciente chamado ainda.</p>
          )}

          {senha !== null && (
            // A key faz a animação repetir a cada paciente novo.
            <article key={senha.id} className="profissional-chamado">
              <p className="profissional-rotulo">Paciente chamado</p>
              <strong className="profissional-numero">{formatarNumero(senha.numero)}</strong>
              <h3>{senha.nomePaciente}</h3>
              <p className="profissional-detalhe">
                {senha.servico} · chamado às {formatarHora(senha.chamadaEm)}
              </p>
              {senha.prioridade !== 'NENHUMA' && (
                <span className="profissional-selo">{textoPrioridade(senha.prioridade)}</span>
              )}
            </article>
          )}
        </div>
      </section>
    </main>
  )
}
