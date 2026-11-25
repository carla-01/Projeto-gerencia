import { useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { fazerLogout } from '../services/auth'
import {
  ErroCheckIn,
  buscarCheckIn,
  colocarNaFila,
  confirmarPresenca,
  listarServicosDaRecepcao,
  marcarPrioridade,
} from '../services/checkIn'
import type { PrioridadeSenha, ResultadoBusca, SenhaCheckIn } from '../services/checkIn'
import { mascararCpf, somenteNumeros } from '../services/cpf'
import type { ServicoDisponivel } from '../services/fila'
import './CheckIn.css'

const TAMANHO_MAXIMO_NUMERO = 4

/** Prioridades que a recepção marca (US11). Sem prioridade é o padrão da senha. */
const PRIORIDADES: PrioridadeSenha[] = ['IDOSO', 'GESTANTE', 'PCD', 'URGENCIA']

const MOTIVO_OBRIGATORIO = 'Informe o motivo da urgência'

/** Até 4 números é o número da senha; com mais, é um CPF e ganha a máscara. */
function mascararBusca(texto: string): string {
  const numeros = somenteNumeros(texto)
  if (numeros.length > TAMANHO_MAXIMO_NUMERO) {
    return mascararCpf(numeros)
  }
  return numeros
}

/** 7 vira "007", como no painel de senhas. */
function formatarNumero(numero: number): string {
  return String(numero).padStart(3, '0')
}

/** "2026-11-24T12:00:00Z" vira "09:00" no horário do navegador. */
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

function IconeBusca() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="11" cy="11" r="6.5" fill="none" stroke="currentColor" strokeWidth="2" />
      <path d="M16 16l4.5 4.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}

function IconeCheck() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function IconeAlerta() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" strokeWidth="1.9" />
      <path d="M12 7.5v5.5" stroke="currentColor" strokeWidth="2.1" strokeLinecap="round" />
      <circle cx="12" cy="16.5" r="1.2" fill="currentColor" />
    </svg>
  )
}

/**
 * Check-in na recepção (US10 - SCRUM-156).
 *
 * A recepcionista busca pelo CPF ou pelo número e confirma a presença (CT-US10-01).
 * Sem senha hoje, aparece "Paciente sem senha hoje" com a opção de colocá-lo na fila (CT-US10-02).
 * Em cada senha, a recepção também marca a prioridade legal (US11 - SCRUM-160); na urgência,
 * o motivo é obrigatório.
 */
export default function CheckIn() {
  const navigate = useNavigate()
  const campoBusca = useRef<HTMLInputElement>(null)
  const [busca, setBusca] = useState('')
  const [resultado, setResultado] = useState<ResultadoBusca | null>(null)
  const [servicos, setServicos] = useState<ServicoDisponivel[] | null>(null)
  const [servicoEscolhido, setServicoEscolhido] = useState<number | null>(null)
  const [erro, setErro] = useState('')
  const [aviso, setAviso] = useState('')
  const [ocupado, setOcupado] = useState(false)
  // Senha com o formulário de prioridade aberto (só uma por vez).
  const [senhaDaPrioridade, setSenhaDaPrioridade] = useState<number | null>(null)
  const [prioridadeEscolhida, setPrioridadeEscolhida] = useState<PrioridadeSenha | null>(null)
  const [motivo, setMotivo] = useState('')
  const [erroMotivo, setErroMotivo] = useState('')

  function tratarErro(e: unknown) {
    if (!(e instanceof ErroCheckIn)) {
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

  /** Apaga o resultado anterior antes de uma busca nova. */
  function limparResultado() {
    setResultado(null)
    setServicos(null)
    setServicoEscolhido(null)
    setErro('')
    setAviso('')
    fecharPrioridade()
  }

  /** Troca no resultado só a senha que mudou; as outras continuam como estavam. */
  function trocarSenha(nova: SenhaCheckIn) {
    if (resultado === null) {
      return
    }
    const senhas: SenhaCheckIn[] = []
    for (const item of resultado.senhas) {
      if (item.id === nova.id) {
        senhas.push(nova)
      } else {
        senhas.push(item)
      }
    }
    setResultado({ ...resultado, senhas: senhas })
  }

  function abrirPrioridade(senha: SenhaCheckIn) {
    setErro('')
    setAviso('')
    setSenhaDaPrioridade(senha.id)
    setPrioridadeEscolhida(senha.prioridade === 'NENHUMA' ? null : senha.prioridade)
    setMotivo('')
    setErroMotivo('')
  }

  function fecharPrioridade() {
    setSenhaDaPrioridade(null)
    setPrioridadeEscolhida(null)
    setMotivo('')
    setErroMotivo('')
  }

  async function salvarPrioridade(senha: SenhaCheckIn) {
    setErro('')
    setAviso('')
    if (prioridadeEscolhida === null) {
      setErro('Escolha a prioridade')
      return
    }
    // Cenário 2: urgência sem motivo não vai para o back-end.
    if (prioridadeEscolhida === 'URGENCIA' && motivo.trim() === '') {
      setErroMotivo(MOTIVO_OBRIGATORIO)
      return
    }
    setErroMotivo('')
    setOcupado(true)
    try {
      const marcada = await marcarPrioridade(senha.id, prioridadeEscolhida, motivo.trim())
      trocarSenha({ ...senha, prioridade: marcada.prioridade })
      fecharPrioridade()
      setAviso('Prioridade ' + textoPrioridade(marcada.prioridade) + ' marcada na senha '
        + formatarNumero(marcada.numero) + '.')
    } catch (e) {
      tratarErro(e)
    }
    setOcupado(false)
  }

  async function buscar(evento: FormEvent) {
    evento.preventDefault()
    limparResultado()
    if (busca === '') {
      setErro('Informe o CPF ou o número da senha')
      return
    }

    setOcupado(true)
    try {
      setResultado(await buscarCheckIn(busca))
    } catch (e) {
      tratarErro(e)
    }
    setOcupado(false)
  }

  async function confirmar(senha: SenhaCheckIn) {
    setErro('')
    setAviso('')
    setOcupado(true)
    try {
      const confirmada = await confirmarPresenca(senha.id)
      trocarSenha(confirmada)
      setAviso('Presença confirmada para ' + confirmada.nomePaciente + '.')
    } catch (e) {
      tratarErro(e)
    }
    setOcupado(false)
  }

  async function abrirServicos() {
    setErro('')
    setOcupado(true)
    try {
      setServicos(await listarServicosDaRecepcao())
    } catch (e) {
      tratarErro(e)
    }
    setOcupado(false)
  }

  async function gerarSenha(pacienteId: number) {
    if (servicoEscolhido === null) {
      setErro('Escolha o serviço')
      return
    }
    setErro('')
    setOcupado(true)
    try {
      const senha = await colocarNaFila(pacienteId, servicoEscolhido)
      // Busca de novo para a senha nova aparecer com o botão de confirmar presença.
      const atualizado = await buscarCheckIn(busca)
      setResultado(atualizado)
      setServicos(null)
      setServicoEscolhido(null)
      setAviso('Senha ' + formatarNumero(senha.numero) + ' gerada para ' + senha.servico + '.')
    } catch (e) {
      tratarErro(e)
    }
    setOcupado(false)
  }

  function novaBusca() {
    limparResultado()
    setBusca('')
    campoBusca.current?.focus()
  }

  function cartaoDaSenha(senha: SenhaCheckIn) {
    const presente = senha.status === 'PRESENTE'
    let classe = 'checkin-senha'
    if (presente) {
      classe = 'checkin-senha checkin-senha-presente'
    }

    return (
      <li key={senha.id} className={classe}>
        <div className="checkin-bilhete">
          <span>Senha</span>
          <strong>{formatarNumero(senha.numero)}</strong>
        </div>

        <div className="checkin-dados">
          <h3>{senha.nomePaciente}</h3>
          <p>
            {senha.servico} · CPF {senha.cpfMascarado}
          </p>
          <div className="checkin-selos">
            {!presente && <span className="checkin-selo checkin-selo-aguardando">Aguardando</span>}
            {presente && <span className="checkin-selo checkin-selo-presente">Presente</span>}
            {senha.prioridade !== 'NENHUMA' && (
              <span className="checkin-selo checkin-selo-prioridade">{textoPrioridade(senha.prioridade)}</span>
            )}
          </div>
        </div>

        <div className="checkin-acao">
          {!presente && (
            <button type="button" className="checkin-botao" disabled={ocupado} onClick={() => confirmar(senha)}>
              <IconeCheck />
              Confirmar presença
            </button>
          )}
          {presente && senha.checkinEm !== null && (
            <p className="checkin-hora">
              <IconeCheck />
              Check-in às {formatarHora(senha.checkinEm)}
            </p>
          )}
          {senhaDaPrioridade !== senha.id && (
            <button type="button" className="checkin-botao-secundario" disabled={ocupado}
              onClick={() => abrirPrioridade(senha)}>
              Marcar prioridade
            </button>
          )}
        </div>

        {senhaDaPrioridade === senha.id && formularioDePrioridade(senha)}
      </li>
    )
  }

  /** Prioridade legal da senha (US11 - SCRUM-160). */
  function formularioDePrioridade(senha: SenhaCheckIn) {
    const idMotivo = 'motivo-' + senha.id
    const idErroMotivo = 'erro-motivo-' + senha.id
    const urgencia = prioridadeEscolhida === 'URGENCIA'

    return (
      <fieldset className="checkin-prioridade">
        <legend>Prioridade da senha {formatarNumero(senha.numero)}</legend>
        <div className="checkin-grade">
          {PRIORIDADES.map((prioridade) => {
            let classe = 'checkin-opcao'
            if (prioridadeEscolhida === prioridade) {
              classe = 'checkin-opcao checkin-opcao-marcada'
            }
            return (
              <label key={prioridade} className={classe}>
                <input
                  type="radio"
                  name={'prioridade-' + senha.id}
                  checked={prioridadeEscolhida === prioridade}
                  onChange={() => {
                    setPrioridadeEscolhida(prioridade)
                    setErroMotivo('')
                  }}
                />
                <strong>{textoPrioridade(prioridade)}</strong>
              </label>
            )
          })}
        </div>

        <label htmlFor={idMotivo} className="checkin-motivo-rotulo">
          {urgencia ? 'Motivo da urgência' : 'Motivo (opcional)'}
        </label>
        <textarea
          id={idMotivo}
          className="checkin-motivo"
          rows={2}
          maxLength={255}
          value={motivo}
          aria-invalid={erroMotivo !== ''}
          aria-describedby={erroMotivo !== '' ? idErroMotivo : undefined}
          onChange={(e) => {
            setMotivo(e.target.value)
            setErroMotivo('')
          }}
        />
        {erroMotivo !== '' && (
          <p id={idErroMotivo} className="checkin-motivo-erro" role="alert">
            {erroMotivo}
          </p>
        )}

        <div className="checkin-prioridade-botoes">
          <button type="button" className="checkin-botao" disabled={ocupado} onClick={() => salvarPrioridade(senha)}>
            Salvar prioridade
          </button>
          <button type="button" className="checkin-link" onClick={fecharPrioridade}>
            Cancelar
          </button>
        </div>
      </fieldset>
    )
  }

  function opcoesDeServico(pacienteId: number, lista: ServicoDisponivel[]) {
    return (
      <fieldset className="checkin-servicos">
        <legend>Em qual serviço?</legend>
        {lista.length === 0 && <p>Nenhum serviço cadastrado.</p>}
        <div className="checkin-grade">
          {lista.map((servico) => {
            let classe = 'checkin-opcao'
            if (servicoEscolhido === servico.id) {
              classe = 'checkin-opcao checkin-opcao-marcada'
            }
            if (!servico.aberta) {
              classe = 'checkin-opcao checkin-opcao-fechada'
            }
            return (
              <label key={servico.id} className={classe}>
                <input
                  type="radio"
                  name="servico"
                  disabled={!servico.aberta}
                  checked={servicoEscolhido === servico.id}
                  onChange={() => setServicoEscolhido(servico.id)}
                />
                <strong>{servico.nome}</strong>
                {servico.aberta && <span>Fila aberta</span>}
                {!servico.aberta && <span>Fila fechada</span>}
              </label>
            )
          })}
        </div>
        <button type="button" className="checkin-botao" disabled={ocupado} onClick={() => gerarSenha(pacienteId)}>
          Gerar senha
        </button>
      </fieldset>
    )
  }

  function semSenha(dados: ResultadoBusca) {
    const pacienteId = dados.pacienteId
    const buscouCpf = somenteNumeros(busca).length > TAMANHO_MAXIMO_NUMERO

    return (
      <div className="checkin-vazio" role="status">
        <span className="checkin-vazio-icone" aria-hidden="true">
          <IconeAlerta />
        </span>
        <h3>{dados.mensagem}</h3>

        {pacienteId !== null && <p>{dados.nomePaciente} tem cadastro na UBS.</p>}
        {pacienteId !== null && servicos === null && (
          <button type="button" className="checkin-botao" disabled={ocupado} onClick={abrirServicos}>
            Colocar na fila
          </button>
        )}
        {pacienteId !== null && servicos !== null && opcoesDeServico(pacienteId, servicos)}

        {pacienteId === null && buscouCpf && (
          <>
            <p>Este CPF ainda não tem cadastro. Cadastre o paciente para colocá-lo na fila.</p>
            <Link to="/recepcao/cadastrar" className="checkin-botao">
              Cadastrar paciente
            </Link>
          </>
        )}
      </div>
    )
  }

  let textoBotao = 'Buscar'
  if (ocupado && resultado === null) {
    textoBotao = 'Buscando...'
  }

  return (
    <section className="checkin" aria-labelledby="checkin-titulo">
      <div className="checkin-topo">
        <h2 id="checkin-titulo">Check-in</h2>
        <p>Confirme a presença de quem chegou à UBS. Só quem está presente é chamado.</p>

        <form className="checkin-form" onSubmit={buscar} noValidate>
          <label htmlFor="busca">CPF ou número da senha</label>
          <div className="checkin-linha">
            <div className="checkin-entrada">
              <IconeBusca />
              <input
                id="busca"
                ref={campoBusca}
                type="text"
                inputMode="numeric"
                autoComplete="off"
                placeholder="000.000.000-00 ou 12"
                value={busca}
                onChange={(e) => setBusca(mascararBusca(e.target.value))}
              />
            </div>
            <button type="submit" className="checkin-buscar" disabled={ocupado}>
              {textoBotao}
            </button>
          </div>
        </form>
      </div>

      <div className="checkin-corpo" aria-live="polite">
        {erro !== '' && (
          <div className="checkin-erro" role="alert">
            <IconeAlerta />
            {erro}
          </div>
        )}
        {aviso !== '' && (
          <div className="checkin-sucesso" role="status">
            <IconeCheck />
            {aviso}
          </div>
        )}

        {resultado === null && erro === '' && (
          <p className="checkin-dica">Digite o CPF do paciente ou o número da senha e toque em Buscar.</p>
        )}

        {resultado !== null && resultado.senhas.length > 0 && (
          <ul className="checkin-lista">{resultado.senhas.map(cartaoDaSenha)}</ul>
        )}

        {resultado !== null && resultado.senhas.length === 0 && semSenha(resultado)}

        {resultado !== null && (
          <button type="button" className="checkin-link" onClick={novaBusca}>
            Nova busca
          </button>
        )}
      </div>
    </section>
  )
}
