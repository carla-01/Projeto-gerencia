import { afterEach, describe, expect, it, vi } from 'vitest'
import { salvarConfiguracaoFila } from './filas'

describe('salvarConfiguracaoFila', () => {
  afterEach(() => {
    localStorage.clear()
    vi.unstubAllGlobals()
  })

  it('envia os dados e o token JWT para o endpoint de serviços', async () => {
    localStorage.setItem('filasus.token', 'token-de-teste')
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 201 })
    vi.stubGlobal('fetch', fetchMock)

    await salvarConfiguracaoFila({
      servico: 'Clínico',
      horaInicio: '07:00',
      horaFim: '12:00',
      limiteSenhas: 40,
    })

    expect(fetchMock).toHaveBeenCalledWith('/api/gestor/servicos', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer token-de-teste',
      },
      body: JSON.stringify({
        servico: 'Clínico',
        horaInicio: '07:00',
        horaFim: '12:00',
        limiteSenhas: 40,
      }),
    })
  })
})
