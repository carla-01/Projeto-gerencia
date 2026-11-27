# language: pt
# SCRUM-157 · US10 Check-in na recepção (SCRUM-45) · endpoints das SCRUM-154 e SCRUM-155
# Executado pelo Cucumber: CucumberTest (passos em br.uepb.filasus.bdd.CheckInSteps)
# O relógio dos cenários fica parado às 09:00 no horário da UBS (CucumberSpringConfig).
# A tela é testada em frontend/src/components/CheckIn.test.tsx (SCRUM-156). Dados fictícios.

Funcionalidade: Check-in na recepção
  Como recepcionista
  Quero confirmar a presença do paciente quando ele chega na UBS
  Para que só quem está presente seja chamado

  Contexto:
    Dado que sou a recepcionista que faz o check-in
    E que o serviço "Clínico" funciona das 07:00 às 17:00 com limite de 40 senhas

  @smoke
  Cenário: CT-US10-01 - Check-in
    Dado que o paciente tem senha ativa no serviço "Clínico"
    Quando busco pelo CPF e confirmo a presença
    Então o status da senha do paciente muda para "PRESENTE"
    E o horário do check-in fica gravado

  Cenário: CT-US10-02 - Senha inexistente
    Dado que o CPF do paciente não tem senha hoje
    Quando tento fazer o check-in pelo CPF
    Então a recepção vê o aviso "Paciente sem senha hoje"
    Quando coloco o paciente na fila do serviço "Clínico"
    Então o paciente recebe a senha número 1 com o status "AGUARDANDO"
