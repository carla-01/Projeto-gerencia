# language: pt
# SCRUM-169 · US14 Iniciar e finalizar atendimento (SCRUM-48) · endpoints da SCRUM-167
# Executado pelo Cucumber: CucumberTest (passos em br.uepb.filasus.bdd.AtendimentoSteps)
# O relógio dos cenários fica parado às 09:00 no horário da UBS (CucumberSpringConfig).
# A tela é testada em PainelProfissional.test.tsx (SCRUM-168). Dados fictícios.

Funcionalidade: Iniciar e finalizar atendimento
  Como profissional de saúde
  Quero iniciar e finalizar o atendimento do paciente chamado
  Para que o tempo de atendimento seja medido

  Contexto:
    Dado que o serviço "Clínico" funciona das 07:00 às 17:00 com limite de 40 senhas
    E que atendo no setor "Clínico"

  @smoke
  Cenário: CT-US14-01 - Ciclo completo
    Dado que chamei o paciente da senha número 1
    Quando clico em iniciar e depois em finalizar
    Então os horários de início e fim ficam gravados
    E a senha número 1 fica com a situação "ATENDIDA"
    E o paciente da senha número 1 sai da fila

  Cenário: CT-US14-02 - Finalizar sem iniciar
    Dado que chamei o paciente da senha número 1
    E que o atendimento não foi iniciado
    Quando tento finalizar
    Então o sistema pede "Inicie o atendimento antes de finalizar"
    E a senha número 1 fica com a situação "CHAMADA"
