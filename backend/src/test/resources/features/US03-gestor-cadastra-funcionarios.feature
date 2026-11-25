# language: pt
# SCRUM-153 · US03 Gestor cadastra funcionários (SCRUM-15)
# Executado pelo Cucumber: CucumberTest (passos em br.uepb.filasus.bdd.CadastrarFuncionarioSteps)

@us03
Funcionalidade: Gestor cadastra funcionários
  Como gestor
  Quero cadastrar recepcionistas e profissionais de saúde com seus papéis
  Para que a equipe da UBS tenha acesso ao sistema

  @smoke
  Cenário: CT-US03-01 - Cadastro de funcionário
    Dado que estou logado como gestor da UBS
    E que a UBS tem o setor "Enfermagem BDD"
    Quando cadastro um profissional com nome "Enfermeira Fictícia", e-mail "enfermeira.bdd@exemplo.test", setor "Enfermagem BDD" e papel "PROFISSIONAL"
    Então ele recebe acesso e aparece na lista de usuários

  Cenário: CT-US03-02 - E-mail duplicado
    Dado que estou logado como gestor da UBS
    E que já existe um usuário com o e-mail "repetido.bdd@exemplo.test"
    Quando tento salvar o cadastro de uma recepcionista com o e-mail "repetido.bdd@exemplo.test"
    Então vejo "E-mail já cadastrado" e nada é gravado
