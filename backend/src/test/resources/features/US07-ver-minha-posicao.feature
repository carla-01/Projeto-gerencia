# language: pt
# SCRUM-141 · US07 Ver minha posição (SCRUM-42) · endpoint da SCRUM-139
# Executado pelo Cucumber: CucumberTest (passos em br.uepb.filasus.bdd.PosicaoFilaSteps)
# O relógio dos cenários fica parado às 09:00 no horário da UBS (CucumberSpringConfig).

Funcionalidade: Ver minha posição
  Como paciente
  Quero ver minha posição e quantas pessoas estão na minha frente
  Para saber quando sair de casa

  @smoke
  Cenário: CT-US07-01 - Posição exibida
    Dado que tenho a senha número 3 na fila do serviço "Clínico"
    E há 2 pessoas aguardando na minha frente
    Quando abro a minha fila
    Então vejo a senha número 3 do serviço "Clínico"
    E estou na posição 3 com 2 pessoas na minha frente

  Cenário: CT-US07-02 - Não estou na fila
    Dado que já fui atendido hoje no serviço "Clínico" e não tenho senha ativa
    Quando abro a minha fila
    Então nenhuma senha aparece para mim
    E vejo a opção de entrar na fila do serviço "Clínico"
