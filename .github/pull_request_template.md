<!-- Título do PR: SCRUM-<chave> - <título curto da alteração> -->
<!-- Base: develop (a main só recebe a develop no fechamento da release) -->

**Issue no Jira:** SCRUM-<chave>
**Branch:** `<feature|docs|ci|fix>/SCRUM-<chave>-<nome>` → `develop`
**Revisora:** <integrante do rodízio>

## O que foi feito

<Descreva em poucas linhas o que esta alteração entrega>

- <alteração 1>
- <alteração 2>

## Por que foi feito

<Qual US, tarefa ou bug isso resolve e qual o valor entregue>

## Tipo de alteração

- [ ] Nova funcionalidade (US)
- [ ] Correção de bug
- [ ] Refatoração
- [ ] Documentação
- [ ] Configuração / CI / infraestrutura

## Como testar

1. <passo 1>
2. <passo 2>
3. <resultado esperado>

Comandos úteis: `mvn test` (back-end), `npm test` (front-end)

## Evidências

<Capturas de tela, GIFs ou resultado dos testes. Sem CPF nem dados reais de pacientes>

## Checklist da autora

- [ ] A branch está atualizada com a develop
- [ ] O código segue o padrão do projeto
- [ ] Testes novos ou atualizados, e todos passando
- [ ] Pipeline de CI sem falhas
- [ ] Critérios de aceite da US atendidos
- [ ] Documentação e README atualizados, se necessário
- [ ] Sem segredos, senhas ou dados sensíveis no código

## Checklist da revisora

- [ ] A base do PR é a develop
- [ ] Conferi "Files changed": nenhum arquivo apagado ou alterado sem motivo
- [ ] Li o código e não encontrei problemas
- [ ] Executei os passos de "Como testar"
- [ ] Critérios de aceite validados
- [ ] CI verde antes de aprovar

**Observações para a revisora:** <pontos de atenção, dúvidas, decisões tomadas>
