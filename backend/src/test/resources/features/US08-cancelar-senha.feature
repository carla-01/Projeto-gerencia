# language: pt
# SCRUM-43 · US08 Cancelar minha senha · endpoint POST /api/paciente/senhas/{id}/cancelar
# Executado pelo Cucumber: CucumberTest (passos em br.uepb.filasus.bdd.CancelarSenhaSteps)
# O relógio dos cenários fica parado às 09:00 no horário da UBS (CucumberSpringConfig).

Funcionalidade: Cancelar minha senha
  Como paciente
  Quero cancelar minha senha se eu não puder mais ir
  Para que a fila ande para quem está esperando

  @smoke
  Cenário: CT-US08-01 - Cancelamento
    Dado que estou na fila do serviço "Clínico" com a senha número 1
    E há mais 2 pacientes aguardando depois de mim
    Quando cancelo a minha senha
    Então a minha senha fica com o status "CANCELADA"
    E restam 2 senhas na fila do serviço

  Cenário: CT-US08-02 - Já fui chamado
    Dado que estou na fila do serviço "Clínico" com a senha número 1
    E o meu atendimento já começou
    Quando cancelo a minha senha
    Então recebo a mensagem "Seu atendimento já começou"
    E a minha senha fica com o status "EM_ATENDIMENTO"
