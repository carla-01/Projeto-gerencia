import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { finalizarAtendimento, iniciarAtendimento } from '../services/atendimento'
import type { Atendimento } from '../services/atendimento'
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

/** Minutos entre o início e o fim, para mostrar o tempo de atendimento. */
function formatarDuracao(inicio: string, fim: string): string {
  const minutos = Math.round((new Date(fim).getTime() - new Date(inicio).getTime()) / 60000)
  if (minutos < 1) {
    return 'menos de 1 min'
  }
  return minutos + ' min'
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
 *
 * Com o paciente chamado, "Iniciar atendimento" e "Finalizar atendimento" gravam os horários
 * (US14 - SCRUM-168). Finalizar sem iniciar mostra "Inicie o atendimento antes de finalizar",
 * que vem do back-end (CT-US14-02). Enquanto o atendimento está em andamento, o próximo não é
 * chamado, para o atendimento aberto não sumir da tela.
 */
export default function PainelProfissional() {
  const navigate = useNavigate()
  const [senha, setSenha] = useState<SenhaChamada | null>(null)
  const [aviso, setAviso] = useState('')
  const [erro, setErro] = useState('')
  const [chamando, setChamando] = useState(false)
  const [atendimento, setAtendimento] = useState<Atendimento | null>(null)
  const [processando, setProcessando] = useState(false)

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
        setAtendimento(null)
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

  async function mudarAtendimento(acao: (senhaId: number) => Promise<Atendimento>) {
    if (senha === null) {
      return
    }
    setErro('')
    setAviso('')
    setProcessando(true)
    try {
      setAtendimento(await acao(senha.id))
    } catch (e) {
      tratarErro(e)
    }
    setProcessando(false)
  }

  // Depois de iniciar ou finalizar, a situação que vale é a do atendimento.
  let situacao = ''
  if (senha !== null) {
    situacao = senha.status
  }
  if (atendimento !== null) {
    situacao = atendimento.status
  }
  const emAtendimento = situacao === 'EM_ATENDIMENTO'

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
          <button
            type="button"
            className="profissional-chamar"
            disabled={chamando || emAtendimento}
            onClick={chamar}
          >
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

              {atendimento !== null && atendimento.status === 'EM_ATENDIMENTO' && (
                <p className="profissional-situacao">
                  Em atendimento desde {formatarHora(atendimento.iniciadoEm)}
                </p>
              )}
              {atendimento !== null && atendimento.finalizadoEm !== null && (
                <p className="profissional-situacao profissional-situacao-fim" role="status">
                  Atendimento finalizado às {formatarHora(atendimento.finalizadoEm)} ·{' '}
                  {formatarDuracao(atendimento.iniciadoEm, atendimento.finalizadoEm)}. O paciente saiu da fila.
                </p>
              )}

              {situacao !== 'ATENDIDA' && (
                <div className="profissional-acoes">
                  <button
                    type="button"
                    className="profissional-iniciar"
                    disabled={processando || emAtendimento}
                    onClick={() => mudarAtendimento(iniciarAtendimento)}
                  >
                    Iniciar atendimento
                  </button>
                  <button
                    type="button"
                    className="profissional-finalizar"
                    disabled={processando}
                    onClick={() => mudarAtendimento(finalizarAtendimento)}
                  >
                    Finalizar atendimento
                  </button>
                </div>
              )}
            </article>
          )}
        </div>
      </section>
    </main>
  )
}
