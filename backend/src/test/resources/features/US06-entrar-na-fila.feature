# language: pt
# SCRUM-138 · US06 Entrar na fila (SCRUM-41) · endpoint da SCRUM-136
# Executado pelo Cucumber: CucumberTest (passos em br.uepb.filasus.bdd.EntradaFilaSteps)
# O relógio dos cenários fica parado às 09:00 no horário da UBS (CucumberSpringConfig).

Funcionalidade: Entrar na fila
  Como paciente
  Quero entrar na fila do serviço pelo celular
  Para não precisar chegar cedo à UBS para pegar senha

  Contexto:
    Dado que sou um paciente cadastrado e logado

  @smoke
  Cenário: CT-US06-01 - Entrar na fila com sucesso
    Dado que o serviço "Clínico" funciona das 07:00 às 17:00 com limite de 40 senhas
    Quando entro na fila do serviço "Clínico"
    Então recebo a senha número 1 do serviço "Clínico"
    E a senha fica com o status "AGUARDANDO"

  Cenário: CT-US06-02 - Fila fechada fora do horário
    Dado que o serviço "Odontologia" funciona das 13:00 às 17:00 com limite de 20 senhas
    Quando entro na fila do serviço "Odontologia"
    Então vejo a mensagem "Fila fechada"
    E nenhuma senha é gerada para mim

  Cenário: CT-US06-03 - Fila fechada quando as senhas do dia acabaram
    Dado que o serviço "Enfermagem" funciona das 07:00 às 17:00 com limite de 1 senha
    E já foi emitida 1 senha hoje no serviço "Enfermagem"
    Quando entro na fila do serviço "Enfermagem"
    Então vejo a mensagem "Fila fechada"
    E nenhuma senha é gerada para mim

  Cenário: CT-US06-04 - Não tirar duas senhas no mesmo serviço
    Dado que o serviço "Clínico" funciona das 07:00 às 17:00 com limite de 40 senhas
    E já entrei na fila do serviço "Clínico"
    Quando entro na fila do serviço "Clínico"
    Então vejo a mensagem "Você já tem uma senha ativa neste serviço"
