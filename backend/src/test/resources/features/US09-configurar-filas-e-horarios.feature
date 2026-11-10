# language: pt
# SCRUM-134 · US09 Configurar filas e horários (SCRUM-44)
# Executado pelo Cucumber: CucumberTest (passos em br.uepb.filasus.bdd.ConfigurarFilasSteps)

@us09
Funcionalidade: Configurar filas e horários
  Como gestor
  Quero configurar os serviços da UBS
  Para definir os horários de funcionamento e o limite diário de senhas

  Cenário: CT-US09-01 - Configuração salva
    Dado que estou autenticado no painel do gestor
    Quando defino o serviço "Clínico" das 07:00 às 12:00 com limite de 40 senhas
    Então a fila abre e fecha nesses horários e limita a 40 senhas

  Cenário: CT-US09-02 - Horário inválido
    Dado que estou autenticado no painel do gestor
    E que já existe o serviço "Clínico" configurado das 07:00 às 12:00 com limite de 40 senhas
    Quando defino o serviço "Clínico" das 12:00 às 07:00 com limite de 20 senhas
    Então recebo a mensagem "Horário inválido" sem salvar ou alterar a configuração
