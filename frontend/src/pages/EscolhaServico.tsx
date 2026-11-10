import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fazerLogout } from '../services/auth'
import { ErroFila, entrarNaFila, listarServicos } from '../services/fila'
import type { SenhaGerada, ServicoDisponivel } from '../services/fila'
import './EscolhaServico.css'

/** "2026-11-10" vira "10/11/2026" (a data já vem no dia local da UBS). */
function formatarData(data: string): string {
  const [ano, mes, dia] = data.split('-')
  return dia + '/' + mes + '/' + ano
}

function IconeAlerta() {
  return (
    <svg className="fila-icone-alerta" viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path d="M12 7.5v5.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <circle cx="12" cy="16.5" r="1.2" fill="currentColor" />
    </svg>
  )
}

function IconeCheck() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

/**
 * Tela de escolha de serviço do paciente (US06 - SCRUM-137).
 *
 * O paciente vê os serviços da UBS, se a fila de cada um está aberta, e entra na fila
 * do serviço escolhido. Fila fechada (fora do horário ou sem senhas) não deixa entrar.
 */
export default function EscolhaServico() {
  const navigate = useNavigate()
  const [servicos, setServicos] = useState<ServicoDisponivel[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [entrandoEm, setEntrandoEm] = useState<number | null>(null)
  const [senha, setSenha] = useState<SenhaGerada | null>(null)

  useEffect(() => {
    let ativo = true
    listarServicos()
      .then((lista) => {
        if (ativo) {
          setServicos(lista)
        }
      })
      .catch((e) => {
        if (ativo) {
          tratarErro(e)
        }
      })
      .finally(() => {
        if (ativo) {
          setCarregando(false)
        }
      })
    return () => {
      ativo = false
    }
    // tratarErro só usa o navigate, que não muda.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function tratarErro(e: unknown) {
    if (!(e instanceof ErroFila)) {
      setErro('Não foi possível entrar na fila agora. Tente de novo.')
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

  async function entrar(servico: ServicoDisponivel) {
    setErro('')
    setEntrandoEm(servico.id)
    try {
      setSenha(await entrarNaFila(servico.id))
    } catch (e) {
      tratarErro(e)
    }
    setEntrandoEm(null)
  }

  return (
    <main className="fila">
      <header className="fila-cabecalho">
        <p className="fila-rotulo">Paciente</p>
        <h1>Área do paciente</h1>
        <p>Escolha o serviço e entre na fila pelo celular.</p>
      </header>

      {senha !== null && (
        <section className="fila-cartao fila-senha" role="status" aria-live="polite">
          <span className="fila-senha-icone" aria-hidden="true">
            <IconeCheck />
          </span>
          <h2>Você entrou na fila</h2>
          <p className="fila-senha-servico">{senha.servico}</p>
          <p className="fila-senha-rotulo">Sua senha</p>
          <p className="fila-senha-numero">{senha.numero}</p>
          <p className="fila-detalhe">
            Senha de {formatarData(senha.data)}. Faça o check-in na recepção quando chegar à UBS.
          </p>
        </section>
      )}

      {erro !== '' && (
        <div className="fila-alerta" role="alert">
          <IconeAlerta />
          {erro}
        </div>
      )}

      {senha === null && (
        <section className="fila-cartao" aria-labelledby="titulo-servicos" aria-busy={carregando}>
          <h2 id="titulo-servicos" className="fila-secao-titulo">Serviços de hoje</h2>

          {carregando && <p className="fila-detalhe">Carregando serviços...</p>}

          {!carregando && servicos.length === 0 && erro === '' && (
            <p className="fila-detalhe">Nenhum serviço disponível hoje.</p>
          )}

          <ul className="fila-lista">
            {servicos.map((servico) => {
              let textoBotao = 'Entrar na fila'
              if (entrandoEm === servico.id) {
                textoBotao = 'Entrando...'
              }
              return (
                <li key={servico.id} className="fila-servico">
                  <div>
                    <p className="fila-servico-nome">{servico.nome}</p>
                    {servico.aberta && <span className="fila-situacao fila-aberta">Fila aberta</span>}
                    {!servico.aberta && <span className="fila-situacao fila-fechada">Fila fechada</span>}
                  </div>
                  <button
                    type="button"
                    className="fila-botao"
                    disabled={!servico.aberta || entrandoEm !== null}
                    aria-label={'Entrar na fila de ' + servico.nome}
                    onClick={() => entrar(servico)}
                  >
                    {textoBotao}
                  </button>
                </li>
              )
            })}
          </ul>
        </section>
      )}
    </main>
  )
}
