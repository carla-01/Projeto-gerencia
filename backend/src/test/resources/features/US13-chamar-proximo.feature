# language: pt
# SCRUM-166 · US13 Chamar o próximo (SCRUM-47) · endpoints das SCRUM-162 e SCRUM-163
# Executado pelo Cucumber: CucumberTest (passos em br.uepb.filasus.bdd.ChamadaSteps)
# O relógio dos cenários fica parado às 09:00 no horário da UBS (CucumberSpringConfig).
# As telas são testadas em PainelProfissional.test.tsx (SCRUM-164) e em
# EscolhaServico.test.tsx (SCRUM-165). Dados fictícios.

Funcionalidade: Chamar o próximo
  Como profissional de saúde
  Quero chamar o próximo paciente da fila do meu setor
  Para não depender de chamar em voz alta

  Contexto:
    Dado que o serviço "Clínico" funciona das 07:00 às 17:00 com limite de 40 senhas
    E que sou o profissional do setor "Clínico"

  @smoke
  Cenário: CT-US13-01 - Chamada
    Dado que há um paciente presente sem prioridade com a senha número 1
    E que há um paciente presente com prioridade de idoso com a senha número 2
    Quando chamo o próximo paciente
    Então a senha número 2 é chamada
    E o paciente da senha número 2 vê na área dele a situação "CHAMADA"

  Cenário: CT-US13-02 - Fila vazia
    Dado que há um paciente sem check-in com a senha número 1
    Quando chamo o próximo paciente
    Então o profissional vê o aviso "Não há pacientes aguardando"
    E o paciente da senha número 1 vê na área dele a situação "AGUARDANDO"

  Cenário: CT-US13-03 - Paciente vê que foi chamado
    Dado que há um paciente presente sem prioridade com a senha número 1
    Quando chamo o próximo paciente
    Então o paciente da senha número 1 vê na área dele a situação "CHAMADA"
    E a área dele mostra o serviço "Clínico" e o número 1

  Cenário: CT-US13-04 - Paciente chamado não tira outra senha
    Dado que há um paciente presente sem prioridade com a senha número 1
    E chamo o próximo paciente
    Quando o paciente da senha número 1 tenta entrar de novo na fila
    Então o sistema não cria outra senha para ele
    E o paciente da senha número 1 vê na área dele a situação "CHAMADA"
