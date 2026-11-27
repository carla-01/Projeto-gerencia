# language: pt
# SCRUM-138 · US06 Entrar na fila (SCRUM-41) · tela da SCRUM-137 · dados fictícios
#
# Os serviços vêm da carga de exemplo do back-end (Clínico, Enfermagem e Odontologia, das 07:00
# às 17:00). Como o CI roda a qualquer hora, este cenário não depende de a fila estar aberta:
# a entrada na fila, a fila fechada e a senha repetida são testadas com relógio fixo nos cenários
# do back-end (backend/src/test/resources/features/US06-entrar-na-fila.feature).

Funcionalidade: Escolha do serviço na área do paciente
  Como paciente
  Quero ver os serviços da UBS e se a fila de cada um está aberta
  Para escolher em qual fila entrar

  @smoke
  Cenário: Paciente vê os serviços da UBS para entrar na fila
    Dado que abri a tela de cadastro
    Quando preencho os dados de um paciente fictício e aceito o termo
    Então vejo o texto "Conta criada"
    Quando entro com o e-mail e a senha cadastrados
    Então vejo o texto "Área do paciente"
    E vejo o serviço "Clínico" com a situação da fila
    E vejo o serviço "Enfermagem" com a situação da fila
    E vejo o serviço "Odontologia" com a situação da fila
