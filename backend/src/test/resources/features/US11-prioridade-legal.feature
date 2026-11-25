# language: pt
# SCRUM-161 · US11 Aplicar prioridade legal (SCRUM-46) · endpoints das SCRUM-158 e SCRUM-159
# Executado pelo Cucumber: CucumberTest (passos em br.uepb.filasus.bdd.PrioridadeLegalSteps)
# O relógio dos cenários fica parado às 09:00 no horário da UBS (CucumberSpringConfig).
# A tela é testada em frontend/src/components/CheckIn.test.tsx (SCRUM-160). Dados fictícios.

Funcionalidade: Aplicar prioridade legal
  Como recepcionista
  Quero marcar prioridade para idosos, gestantes, pessoas com deficiência e urgências
  Para que a fila respeite a lei e os casos urgentes

  Contexto:
    Dado que sou a recepcionista que marca a prioridade
    E que o serviço "Clínico" funciona das 07:00 às 17:00 com limite de 40 senhas

  @smoke
  Cenário: CT-US11-01 - Prioridade aplicada
    Dado que o paciente está na fila do serviço "Clínico" com a senha número 3
    E há 2 pessoas sem prioridade na frente dele
    Quando marco a prioridade "Idoso 60+"
    Então a senha fica com a prioridade "IDOSO"
    E a mudança fica registrada com a recepcionista e o horário
    E o paciente passa para a posição 1 com 0 pessoas na frente

  Cenário: CT-US11-02 - Prioridade sem justificativa
    Dado que o paciente está na fila do serviço "Clínico" com a senha número 1
    Quando tento marcar a prioridade "Urgência" sem informar o motivo
    Então o sistema exige o motivo com a mensagem "Informe o motivo da urgência"
    E a senha continua sem prioridade
