import { useState } from 'react'
import type { FormEvent } from 'react'
import { salvarConfiguracaoFila } from '../services/filas'
import type { DadosConfiguracaoFila } from '../services/filas'
import './ConfigurarFilas.css'

type CamposConfiguracao = {
  servico: string
  horaInicio: string
  horaFim: string
  limiteSenhas: string
}

const CAMPOS_VAZIOS: CamposConfiguracao = {
  servico: '',
  horaInicio: '',
  horaFim: '',
  limiteSenhas: '',
}

function minutosDoDia(horario: string): number {
  const [horas, minutos] = horario.split(':').map(Number)
  return horas * 60 + minutos
}

export default function ConfigurarFilas() {
  const [campos, setCampos] = useState<CamposConfiguracao>(CAMPOS_VAZIOS)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [enviando, setEnviando] = useState(false)

  function alterar(campo: keyof CamposConfiguracao, valor: string) {
    setCampos({ ...campos, [campo]: valor })
    setErro('')
    setSucesso('')
  }

  async function salvar(evento: FormEvent) {
    evento.preventDefault()
    setErro('')
    setSucesso('')

    if (
      campos.servico.trim() === '' ||
      campos.horaInicio === '' ||
      campos.horaFim === '' ||
      campos.limiteSenhas === ''
    ) {
      setErro('Preencha todos os campos obrigatórios.')
      return
    }

    if (minutosDoDia(campos.horaFim) <= minutosDoDia(campos.horaInicio)) {
      setErro('Horário inválido')
      return
    }

    const limiteSenhas = Number(campos.limiteSenhas)
    if (!Number.isInteger(limiteSenhas) || limiteSenhas <= 0) {
      setErro('O limite de senhas deve ser um inteiro maior que zero.')
      return
    }

    const dados: DadosConfiguracaoFila = {
      servico: campos.servico.trim(),
      horaInicio: campos.horaInicio,
      horaFim: campos.horaFim,
      limiteSenhas,
    }

    setEnviando(true)
    try {
      await salvarConfiguracaoFila(dados)
      setSucesso('Configuração salva com sucesso.')
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível salvar a configuração agora. Tente de novo.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <main className="gestor-config">
      <header className="gestor-config-cabecalho">
        <p className="gestor-config-rotulo">Área do gestor</p>
        <h1>Configurar filas e horários</h1>
        <p>Defina o serviço, o horário de funcionamento e a capacidade diária da UBS.</p>
      </header>

      <form className="gestor-config-form" onSubmit={salvar} noValidate>
        <div className="gestor-config-campo">
          <label htmlFor="servico">Serviço</label>
          <input
            id="servico"
            type="text"
            value={campos.servico}
            onChange={(evento) => alterar('servico', evento.target.value)}
            placeholder="Ex.: Clínico"
            required
          />
        </div>

        <div className="gestor-config-linha">
          <div className="gestor-config-campo">
            <label htmlFor="horaInicio">Horário de início</label>
            <input
              id="horaInicio"
              type="time"
              value={campos.horaInicio}
              onChange={(evento) => alterar('horaInicio', evento.target.value)}
              required
            />
          </div>
          <div className="gestor-config-campo">
            <label htmlFor="horaFim">Horário de fim</label>
            <input
              id="horaFim"
              type="time"
              value={campos.horaFim}
              onChange={(evento) => alterar('horaFim', evento.target.value)}
              required
            />
          </div>
        </div>

        <div className="gestor-config-campo">
          <label htmlFor="limiteSenhas">Limite de senhas por dia</label>
          <input
            id="limiteSenhas"
            type="number"
            min="1"
            step="1"
            value={campos.limiteSenhas}
            onChange={(evento) => alterar('limiteSenhas', evento.target.value)}
            required
          />
        </div>

        {erro !== '' && <p className="gestor-config-mensagem gestor-config-erro" role="alert">{erro}</p>}
        {sucesso !== '' && <p className="gestor-config-mensagem gestor-config-sucesso" role="status">{sucesso}</p>}

        <button type="submit" disabled={enviando}>
          {enviando ? 'Salvando...' : 'Salvar'}
        </button>
      </form>
    </main>
  )
}
