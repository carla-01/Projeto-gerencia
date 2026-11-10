import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'

const SENHA_FICTICIA = 'SenhaFicticia123'

let emailCadastrado = ''

/** Calcula um dígito verificador do CPF a partir dos números já conhecidos. */
function digitoVerificador(numeros: number[]): number {
  let soma = 0
  let peso = numeros.length + 1
  for (const numero of numeros) {
    soma = soma + numero * peso
    peso = peso - 1
  }
  const resto = soma % 11
  if (resto < 2) {
    return 0
  }
  return 11 - resto
}

/** Gera um CPF fictício válido e diferente a cada execução. */
function gerarCpfFicticio(): string {
  const numeros: number[] = []
  for (let i = 0; i < 9; i++) {
    numeros.push(Math.floor(Math.random() * 10))
  }
  numeros.push(digitoVerificador(numeros))
  numeros.push(digitoVerificador(numeros))
  return numeros.join('')
}

/** Gera os 15 números de um cartão SUS fictício. */
function gerarCartaoSusFicticio(): string {
  let cartao = '7'
  for (let i = 0; i < 14; i++) {
    cartao = cartao + Math.floor(Math.random() * 10)
  }
  return cartao
}

Given('que abri a tela de cadastro', () => {
  cy.visit('/cadastro')
})

When('preencho os dados de um paciente fictício e aceito o termo', () => {
  emailCadastrado = 'paciente.' + Date.now() + '@exemplo.test'

  cy.get('#nome').type('Paciente Ficticio')
  cy.get('#cpf').type(gerarCpfFicticio())
  cy.get('#cartaoSus').type(gerarCartaoSusFicticio())
  cy.get('#email').type(emailCadastrado)
  cy.get('#senha').type(SENHA_FICTICIA)
  cy.get('input[type="checkbox"]').check()
  cy.contains('button', 'Criar conta').click()
})

When('entro com o e-mail e a senha cadastrados', () => {
  cy.visit('/login')
  cy.get('#login').type(emailCadastrado)
  cy.get('#senha').type(SENHA_FICTICIA)
  cy.contains('button', 'Entrar').click()
})

Then('vejo o texto {string}', (texto: string) => {
  // O primeiro login demora mais porque a API confere a senha com bcrypt.
  cy.contains(texto, { timeout: 30000 }).should('be.visible')
})
