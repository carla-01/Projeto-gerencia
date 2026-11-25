# language: pt
# SCRUM-124 (CT-US02-04) · US02 Controle de acesso por perfil (SCRUM-38) · DA-SEG-I1-01
# Executado pelo Cucumber: CucumberTest (passos em br.uepb.filasus.bdd.ControleDeAcessoSteps)

Funcionalidade: Registro das tentativas de acesso negado
  Como gestora da UBS
  Quero que toda tentativa de acesso negado fique registrada no log
  Para poder auditar quem tentou acessar uma tela fora do seu perfil

  @smoke
  Cenário: CT-US02-04 - Tentativa negada registrada no log
    Dado que estou logado como paciente
    Quando tento acessar a rota do painel da recepção "/api/recepcao/fila"
    Então o sistema responde 403
    E o log registra "Acesso negado" com o perfil "PACIENTE", o método "GET" e a rota "/api/recepcao/fila"
    E o log não mostra o token de acesso
