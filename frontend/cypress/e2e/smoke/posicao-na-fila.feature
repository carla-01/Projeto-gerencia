# language: pt
# SCRUM-141 · US07 Ver minha posição (SCRUM-42) · tela da SCRUM-140 · dados fictícios
#
# O paciente recém-cadastrado não tem senha, então este cenário não depende de a fila estar
# aberta na hora em que o CI roda. A senha com a posição (CT-US07-01) é testada com relógio
# fixo nos cenários do back-end (backend/src/test/resources/features/US07-ver-minha-posicao.feature)
# e na tela pelos testes do EscolhaServico.test.tsx.

Funcionalidade: Posição na fila na área do paciente
  Como paciente
  Quero ver minha posição e quantas pessoas estão na minha frente
  Para saber quando sair de casa

  @smoke
  Cenário: CT-US07-02 - Paciente sem senha vê a opção de entrar na fila
    Dado que abri a tela de cadastro
    Quando preencho os dados de um paciente fictício e aceito o termo
    Então vejo o texto "Conta criada"
    Quando entro com o e-mail e a senha cadastrados
    Então vejo o texto "Área do paciente"
    E vejo a opção "Entrar na fila" no serviço "Clínico"
    E não vejo nenhuma senha minha
