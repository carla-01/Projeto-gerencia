---
name: História de usuário
about: Nova US no formato "Eu como... preciso... de modo que..." com critérios em Gherkin
title: "[USxx] <título curto>"
labels: user-story
---

<!-- A US oficial fica no Jira. Use este modelo para escrever a descrição e colar lá. -->

**Chave no Jira:** SCRUM-<chave>
**Feature / Épico:** <nome do épico>
**Release:** <R2 - Fila e Atendimento> · **Sprint:** <n> (<início> a <fim>)
**Prioridade:** <Mais alta | Alta | Média | Baixa> · **Story points:** <n>
**Responsável:** <dev>
**Papel de revisão:** PR revisado por <integrante> · QA (<nome>) verifica a DoD · PO (<nome>) dá o aceite

## História de usuário

Eu como <perfil>,
preciso <o que a pessoa precisa fazer>,
de modo que <valor que isso entrega>.

## Contexto e observações

- Branch: `feature/SCRUM-<chave>-<nome>`
- Casos de teste da QA: CT-USxx-01, CT-USxx-02
- Dependências: <histórias de que esta depende, com a chave>

## Critérios de aceite (Gherkin)

```gherkin
Cenário 1: <caso de sucesso>
  Dado que <situação inicial>
  Quando <ação>
  Então <resultado esperado>

Cenário 2: <caso de exceção>
  Dado que <situação inicial>
  Quando <ação>
  Então <resultado esperado>
```

## Definition of Ready (DoR) - preencher antes da Sprint Planning

- [ ] História escrita no formato "Eu como... preciso... de modo que..."
- [ ] Critérios de aceite escritos em Gherkin
- [ ] Estimativa de story points definida pela equipe
- [ ] Prioridade definida pela PO
- [ ] Dependências identificadas
- [ ] Cabe em uma sprint

## Definition of Done (DoD) - conferir na Sprint Review

- [ ] Código revisado e integrado via Pull Request (revisado por outra integrante)
- [ ] Critérios de aceite atendidos e validados pela QA
- [ ] Testes escritos e passando (cenários Gherkin automatizados)
- [ ] Pipeline de CI sem falhas
- [ ] Implantado em ambiente de homologação
- [ ] Sem dado real de paciente, senha ou token no código
- [ ] Conferência da DoD registrada pela QA em comentário na issue
- [ ] Aceita pela PO
