# FilaSUS

[![CI](https://github.com/carla-01/Projeto-gerencia/actions/workflows/ci.yml/badge.svg)](https://github.com/carla-01/Projeto-gerencia/actions/workflows/ci.yml)

Projeto da disciplina de Gerência de Projetos (UEPB, 2026.2).

## Problema

Em muitas UBS, o paciente chega de madrugada, pega senha e espera horas sem saber quando será atendido. A unidade, por sua vez, não enxerga a demanda do dia: não sabe quantas pessoas estão aguardando, por quanto tempo e nem quais são as prioritárias.

## Equipe

- Adrielly Carla Ferreira de Melo
- Maria Eduarda da Nóbrega
- Mirelle Casimiro Silvino

## Papéis por release

| Release | Período | PO + DevOps | Scrum Master | QA |
| --- | --- | --- | --- | --- |
| R1 - Acesso e Cadastro | 06/10 a 29/10/2026 | Adrielly | Maria Eduarda | Mirelle |
| R2 - Fila e Atendimento | 03/11 a 26/11/2026 | Maria Eduarda | Mirelle | Adrielly |
| R3 - Tempo real e Qualidade | 01/12 a 22/12/2026 | Mirelle | Adrielly | Maria Eduarda |
| R4 - Gestão e Demo Day | 02/02 a 18/02/2027 | Adrielly | Maria Eduarda | Mirelle |

Todas as integrantes fazem parte do Dev Team em todas as releases.

## Responsabilidades de cada papel

| Papel | Responsabilidades |
| --- | --- |
| **PO (Product Owner)** | Mantém e prioriza o backlog no Jira; escreve as US no formato "Eu como... preciso... de modo que..." com critérios de aceite em Gherkin; responde dúvidas de regra em até 24 h; aceita ou rejeita cada US na Review; escreve as release notes |
| **DevOps** (junto com o PO) | Mantém o repositório, as branches protegidas (main e develop) e a integração GitHub + Jira; configura o pipeline de CI no GitHub Actions; faz o deploy em homologação; mantém o backup e a documentação de infraestrutura |
| **SM (Scrum Master)** | Facilita Planning, Daily, Review e Retrospectiva; registra as atas; remove impedimentos; acompanha burndown e velocity; mantém a Definition of Done e a planilha de riscos atualizadas |
| **QA (Quality Assurance)** | Mantém o plano de qualidade e o plano de teste; transforma os critérios Gherkin em casos de teste (Cucumber + Cypress); executa os testes e a regressão; registra bugs com severidade no modelo da disciplina; confere a DoD de cada US |
| **Dev Team** (todas) | Desenvolve as US com branches e commits com a chave do Jira; abre PR no modelo da disciplina; revisa o PR da colega no rodízio |

## Links

- Jira (backlog): https://filasusgp.atlassian.net/jira/software/projects/SCRUM/boards/1/backlog
- Modelos de issue e PR: pasta `issue_template/` e arquivo `pull-request-template.txt`