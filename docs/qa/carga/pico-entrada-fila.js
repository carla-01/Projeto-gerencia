/*
 * FilaSUS - teste de carga do pico de entrada na fila (SCRUM-62)
 * Driver DA-DES-I2-01 · Q-10 do Plano de Qualidade · US06 (Entrar na fila)
 *
 * Meta: com 300 pacientes entrando na fila em 5 minutos, 95% recebem a senha em
 * menos de 500 ms (p95), sem erros e sem senha repetida.
 *
 * Como rodar (homologação local, docs/devops/homologacao.txt):
 *   1. Subir o back-end (cd backend e mvn spring-boot:run) com o PostgreSQL ligado.
 *   2. Criar pela tela do gestor (Configurar filas) o serviço "Teste de Carga", das 00:00
 *      às 23:59, com limite de 300 senhas. Os serviços padrão não servem: o limite do dia
 *      deles (Clínico 40, Enfermagem 60, Odontologia 20) barra as 300 entradas (US09).
 *   3. Na raiz do repositório:
 *        k6 run --summary-export docs/qa/carga/resultado-pico.json docs/qa/carga/pico-entrada-fila.js
 *      Outro serviço (aberto e com limite de pelo menos 300):
 *        k6 run -e SERVICO="Nome do serviço" docs/qa/carga/pico-entrada-fila.js
 *   4. Conferir senhas repetidas no banco com docs/qa/carga/verificar-senhas-repetidas.sql.
 *   5. Apagar os dados do teste com docs/qa/carga/limpar-dados-de-carga.sql.
 *
 * Preparação (setup): antes da carga, o script cadastra 300 pacientes fictícios pelo
 * /pacientes/autocadastro e faz o login de cada um, porque cada paciente só pode ter
 * uma senha ativa por serviço. Esse tempo não entra na medição.
 * Dados: só fictícios. CPFs válidos só no cálculo dos dígitos, e-mails @exemplo.test.
 */
import http from 'k6/http'
import exec from 'k6/execution'
import { check, fail } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const SERVICO = __ENV.SERVICO || 'Teste de Carga'
const PACIENTES = 300
const SENHA_DOS_PACIENTES = 'carga1234'

// Identifica a rodada: os CPFs e e-mails mudam a cada execução, então dá para rodar de novo.
const RODADA = String(Date.now() % 100000).padStart(5, '0')

export const options = {
  setupTimeout: '10m',
  scenarios: {
    pico_entrada_fila: {
      // 300 entradas em 5 minutos: 1 por segundo, cada uma de um paciente diferente.
      executor: 'constant-arrival-rate',
      rate: 1,
      timeUnit: '1s',
      duration: '5m',
      preAllocatedVUs: 20,
      maxVUs: 50,
    },
  },
  thresholds: {
    // Driver DA-DES-I2-01: p95 abaixo de 500 ms e nenhum erro.
    'http_req_duration{nome:entrar_na_fila}': ['p(95)<500'],
    'http_req_failed{nome:entrar_na_fila}': ['rate==0'],
    'checks{nome:entrar_na_fila}': ['rate==1'],
  },
}

/** Dígito verificador do CPF (mesma regra do back-end). */
function digito(numeros) {
  let soma = 0
  for (let i = 0; i < numeros.length; i++) {
    soma += Number(numeros[i]) * (numeros.length + 1 - i)
  }
  const resto = soma % 11
  return resto < 2 ? '0' : String(11 - resto)
}

/** CPF fictício do paciente i desta rodada: 5 números da rodada + 4 do paciente + 2 dígitos. */
function cpfFicticio(i) {
  const base = RODADA + String(i).padStart(4, '0')
  const comPrimeiro = base + digito(base)
  return comPrimeiro + digito(comPrimeiro)
}

/** Cabeçalho e tag das chamadas da preparação, que ficam fora da medição. */
const PREPARACAO = { headers: { 'Content-Type': 'application/json' }, tags: { nome: 'preparacao' } }

export function setup() {
  const tokens = []
  for (let i = 0; i < PACIENTES; i++) {
    const email = 'carga.' + RODADA + '.' + i + '@exemplo.test'
    const cadastro = http.post(BASE_URL + '/pacientes/autocadastro', JSON.stringify({
      nome: 'Paciente Carga ' + (i + 1),
      cpf: cpfFicticio(i),
      cartaoSus: '700' + RODADA + String(i).padStart(7, '0'),
      email: email,
      senha: SENHA_DOS_PACIENTES,
      aceiteTermo: true,
    }), PREPARACAO)
    if (cadastro.status !== 201) {
      fail('Cadastro do paciente ' + (i + 1) + ' falhou: ' + cadastro.status + ' ' + cadastro.body)
    }

    const login = http.post(BASE_URL + '/auth/login', JSON.stringify({ login: email, senha: SENHA_DOS_PACIENTES }),
      PREPARACAO)
    if (login.status !== 200) {
      fail('Login do paciente ' + (i + 1) + ' falhou: ' + login.status)
    }
    tokens.push(login.json('token'))
  }

  const servicos = http.get(BASE_URL + '/api/paciente/servicos', {
    headers: { Authorization: 'Bearer ' + tokens[0] },
    tags: { nome: 'preparacao' },
  })
  const servico = servicos.json().find((item) => item.nome === SERVICO)
  if (servico === undefined) {
    fail('Serviço "' + SERVICO + '" não encontrado. Crie pela tela do gestor (passo 2 do cabeçalho).')
  }
  if (!servico.aberta) {
    fail('A fila do serviço "' + SERVICO + '" está fechada agora. Use um serviço aberto (-e SERVICO=...).')
  }

  console.log('Rodada ' + RODADA + ': ' + tokens.length + ' pacientes prontos para o serviço "'
    + SERVICO + '" (id ' + servico.id + ').')
  return { tokens: tokens, servicoId: servico.id }
}

export default function (dados) {
  // Cada iteração usa um paciente diferente: a 1ª o paciente 0, a 2ª o paciente 1...
  const indice = exec.scenario.iterationInTest
  if (indice >= dados.tokens.length) {
    return
  }

  const resposta = http.post(BASE_URL + '/api/paciente/senhas', JSON.stringify({ servicoId: dados.servicoId }), {
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + dados.tokens[indice],
    },
    tags: { nome: 'entrar_na_fila' },
  })

  check(resposta, {
    'senha gerada (201)': (r) => r.status === 201,
    'senha com número': (r) => r.status === 201 && r.json('numero') > 0,
  }, { nome: 'entrar_na_fila' })
}
