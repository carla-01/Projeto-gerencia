import { afterEach, describe, expect, it, vi } from 'vitest'
import { salvarConfiguracaoFila } from './filas'

const DADOS = {
  servico: 'Clínico',
  horaInicio: '07:00',
  horaFim: '12:00',
  limiteSenhas: 40,
}

describe('salvarConfiguracaoFila', () => {
  afterEach(() => {
    localStorage.clear()
    vi.unstubAllGlobals()
  })

  it('envia os dados com os nomes do back-end e o token JWT', async () => {
    localStorage.setItem('filasus.token', 'token-de-teste')
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 201 })
    vi.stubGlobal('fetch', fetchMock)

    await salvarConfiguracaoFila(DADOS)

    expect(fetchMock).toHaveBeenCalledWith('/api/gestor/servicos', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer token-de-teste',
      },
      body: JSON.stringify({
        nome: 'Clínico',
        horarioAbertura: '07:00',
        horarioFechamento: '12:00',
        limiteSenhasDia: 40,
      }),
    })
  })

  it('mostra a mensagem do back-end quando o serviço já existe', async () => {
    localStorage.setItem('filasus.token', 'token-de-teste')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 409,
      json: async () => ({ mensagem: 'Serviço já cadastrado' }),
    }))

    await expect(salvarConfiguracaoFila(DADOS)).rejects.toThrow('Serviço já cadastrado')
  })
})