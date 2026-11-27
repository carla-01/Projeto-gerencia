import { Then } from '@badeball/cypress-cucumber-preprocessor'

// Os passos de cadastro e login são os de entrada-do-paciente.ts.

Then('vejo a opção {string} no serviço {string}', (texto: string, nome: string) => {
  // Sem senha no serviço, o botão mostra "Entrar na fila" (com senha, mostraria "Na fila").
  cy.contains('li', nome, { timeout: 30000 }).within(() => {
    cy.get('button').should('have.text', texto)
  })
})

Then('não vejo nenhuma senha minha', () => {
  // Os serviços já apareceram no passo anterior; espera também a busca das senhas terminar.
  cy.contains('Carregando serviços...').should('not.exist')
  cy.contains('h2', 'Sua senha').should('not.exist')
  cy.get('[role="alert"]').should('not.exist')
})
