import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fazerLogout } from '../services/auth'
import { ErroFila, entrarNaFila, listarMinhasSenhas, listarServicos } from '../services/fila'
import type { PosicaoNaFila, ServicoDisponivel } from '../services/fila'
import './EscolhaServico.css'

/** "2026-11-10" vira "10/11/2026" (a data já vem no dia local da UBS). */
function formatarData(data: string): string {
  const [ano, mes, dia] = data.split('-')
  return dia + '/' + mes + '/' + ano
}

/** Quantas pessoas faltam, do jeito que o paciente lê. */
function textoPessoasAFrente(pessoas: number): string {
  if (pessoas === 0) {
    return 'Você é o próximo da fila'
  }
  if (pessoas === 1) {
    return '1 pessoa na sua frente'
  }
  return pessoas + ' pessoas na sua frente'
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
    <svg className="fila-icone-alerta" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

/** Senha do paciente com a posição e quantas pessoas estão na frente (US07). */
function CartaoSenha({ senha }: { senha: PosicaoNaFila }) {
  const idTitulo = 'senha-' + senha.id
  let orientacao = 'Faça o check-in na recepção quando chegar à UBS.'
  if (senha.status === 'PRESENTE') {
    orientacao = 'Check-in feito. Aguarde ser chamado.'
  }
  return (
    <section className="fila-cartao fila-senha" aria-labelledby={idTitulo}>
      <h2 id={idTitulo}>Sua senha</h2>
      <p className="fila-senha-servico">{senha.servico}</p>
      <div className="fila-senha-dados">
        <div>
          <p className="fila-senha-rotulo">Senha</p>
          <p className="fila-senha-numero">{senha.numero}</p>
        </div>
        <div>
          <p className="fila-senha-rotulo">Posição</p>
          <p className="fila-senha-numero">{senha.posicao}º</p>
        </div>
      </div>
      <p className="fila-senha-frente">{textoPessoasAFrente(senha.pessoasAFrente)}</p>
      <p className="fila-detalhe">
        Senha de {formatarData(senha.data)}. {orientacao}
      </p>
    </section>
  )
}

/**
 * Área do paciente: escolha de serviço (US06 - SCRUM-137) e posição na fila (US07 - SCRUM-140).
 *
 * O paciente vê a senha que já tem, a posição e quantas pessoas estão na frente. Sem senha
 * ativa, vê os serviços da UBS e entra na fila do escolhido. Fila fechada (fora do horário
 * ou sem senhas) não deixa entrar.
 */
export default function EscolhaServico() {
  const navigate = useNavigate()
  const [servicos, setServicos] = useState<ServicoDisponivel[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [entrandoEm, setEntrandoEm] = useState<number | null>(null)
  const [senhas, setSenhas] = useState<PosicaoNaFila[]>([])
  const [aviso, setAviso] = useState('')
  const [atualizando, setAtualizando] = useState(false)

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
    // Separado dos serviços: se a posição falhar, a lista de serviços continua aparecendo.
    listarMinhasSenhas()
      .then((lista) => {
        if (ativo) {
          setSenhas(lista)
        }
      })
      .catch((e) => {
        if (ativo) {
          tratarErro(e)
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
    setAviso('')
    setEntrandoEm(servico.id)
    try {
      const senha = await entrarNaFila(servico.id)
      setAviso('Você entrou na fila de ' + senha.servico + '.')
      // Busca de novo para mostrar a posição da senha nova.
      setSenhas(await listarMinhasSenhas())
    } catch (e) {
      tratarErro(e)
    }
    setEntrandoEm(null)
  }

  async function atualizar() {
    setErro('')
    setAtualizando(true)
    try {
      setSenhas(await listarMinhasSenhas())
    } catch (e) {
      tratarErro(e)
    }
    setAtualizando(false)
  }

  return (
    <main className="fila">
      <header className="fila-cabecalho">
        <p className="fila-rotulo">Paciente</p>
        <h1>Área do paciente</h1>
        <p>Escolha o serviço e entre na fila pelo celular.</p>
      </header>

      {aviso !== '' && (
        <p className="fila-aviso" role="status">
          <IconeCheck />
          {aviso}
        </p>
      )}

      {erro !== '' && (
        <div className="fila-alerta" role="alert">
          <IconeAlerta />
          {erro}
        </div>
      )}

      {senhas.length > 0 && (
        <div className="fila-minhas-senhas">
          {senhas.map((senha) => (
            <CartaoSenha key={senha.id} senha={senha} />
          ))}
          <button type="button" className="fila-botao-secundario" disabled={atualizando} onClick={atualizar}>
            {atualizando ? 'Atualizando...' : 'Atualizar posição'}
          </button>
        </div>
      )}

      <section className="fila-cartao" aria-labelledby="titulo-servicos" aria-busy={carregando}>
        <h2 id="titulo-servicos" className="fila-secao-titulo">Serviços de hoje</h2>

        {carregando && <p className="fila-detalhe">Carregando serviços...</p>}

        {!carregando && servicos.length === 0 && erro === '' && (
          <p className="fila-detalhe">Nenhum serviço disponível hoje.</p>
        )}

        <ul className="fila-lista">
          {servicos.map((servico) => {
            // O nome do serviço é único (US09), então dá para comparar pela senha.
            const jaEstaNaFila = senhas.some((senha) => senha.servico === servico.nome)
            let textoBotao = 'Entrar na fila'
            if (jaEstaNaFila) {
              textoBotao = 'Na fila'
            } else if (entrandoEm === servico.id) {
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
                  disabled={!servico.aberta || jaEstaNaFila || entrandoEm !== null}
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
    </main>
  )
}
