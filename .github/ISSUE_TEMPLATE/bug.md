---
name: Relato de bug
about: Bug encontrado nos testes, vinculado à US e com severidade
title: "[BUG] <sintoma curto> - SCRUM-<chave da US> USxx"
labels: bug
---

<!-- O bug oficial fica no Jira, vinculado à US. Use este modelo para escrever a descrição e colar lá. -->

**Release:** <R2 - Fila e Atendimento> · **Sprint:** <n>
**Severidade:** <Crítica | Alta | Média | Baixa>
**Prioridade:** <Mais alta (Crítica ou Alta) | Alta | Média | Baixa>
**Responsável pela correção:** <dev da US>
**Reportado por:** <nome> (QA)
**US relacionada:** SCRUM-<chave> USxx - <título> · casos de teste CT-USxx-01, CT-USxx-02

## Descrição

<O que está acontecendo de errado, em uma ou duas frases>

## Ambiente

- Ambiente: <local | homologação>
- Navegador / versão: <ex.: Chrome 130>
- Sistema operacional: <ex.: Windows 11>
- Versão / commit / branch: <develop ou feature/SCRUM-<chave>-<nome>>, <commit>

## Passos para reproduzir

1. <passo 1>
2. <passo 2>
3. <passo 3>

## Resultado esperado

<Caso de teste que falhou e o que ele espera, ex.: CT-USxx-01 (nome): resultado esperado>

## Resultado obtido

<O que aconteceu de fato>

## Evidências

<Print ou vídeo anexado, logs ou mensagens de erro. Sem CPF nem dados reais de pacientes>

## Critério de correção (Gherkin, opcional)

```gherkin
CT-USxx-01 - <nome do caso>
  Dado que <situação inicial>
  Quando <ação>
  Então <resultado esperado>
```

## Checklist de encerramento

- [ ] Causa identificada e registrada nos comentários
- [ ] Correção feita na branch da US com commit "SCRUM-<chave do bug> fix(...): ..."
- [ ] Teste de regressão criado ou atualizado
- [ ] Correção verificada pela QA: "Reteste OK" e bug em Concluído
