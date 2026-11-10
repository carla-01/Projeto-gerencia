# language: pt
# SCRUM-123 · US04 (SCRUM-39) e US01 (SCRUM-37) · dados fictícios

Funcionalidade: Entrada do paciente no sistema
  Como paciente
  Quero criar minha conta e entrar no sistema
  Para poder usar a fila virtual

  @smoke
  Cenário: Paciente se cadastra e entra com o e-mail
    Dado que abri a tela de cadastro
    Quando preencho os dados de um paciente fictício e aceito o termo
    Então vejo o texto "Conta criada"
    Quando entro com o e-mail e a senha cadastrados
    Então vejo o texto "Área do paciente"
