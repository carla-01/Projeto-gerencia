import { Then } from '@badeball/cypress-cucumber-preprocessor'

// Os passos de cadastro e login são os de entrada-do-paciente.ts.

Then('vejo o serviço {string} com a situação da fila', (nome: string) => {
  // Cada serviço aparece com "Fila aberta" ou "Fila fechada" e o botão de entrar na fila.
  cy.contains('li', nome, { timeout: 30000 }).within(() => {
    cy.contains(/^Fila (aberta|fechada)$/).should('be.visible')
    cy.get('button').should('have.attr', 'aria-label', 'Entrar na fila de ' + nome)
  })
})
