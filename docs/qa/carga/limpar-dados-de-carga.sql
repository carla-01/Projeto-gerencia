-- =====================================================================
-- FilaSUS - apaga os dados fictícios do teste de carga (SCRUM-62)
-- Remove só os pacientes com e-mail carga.*@exemplo.test, o que aponta para eles
-- e o serviço "Teste de Carga" criado para o teste.
-- Rodar depois da conferência, no banco filasus:
--     psql -U filasus -d filasus -f docs/qa/carga/limpar-dados-de-carga.sql
-- Tudo numa transação: se der erro no meio, nada é apagado.
-- =====================================================================

BEGIN;

CREATE TEMP TABLE pacientes_da_carga ON COMMIT DROP AS
SELECT id AS paciente_id, usuario_id
FROM paciente
WHERE email LIKE 'carga.%@exemplo.test';

DELETE FROM senha WHERE paciente_id IN (SELECT paciente_id FROM pacientes_da_carga);
DELETE FROM consentimento WHERE paciente_id IN (SELECT paciente_id FROM pacientes_da_carga);
DELETE FROM auditoria_acesso
WHERE paciente_id IN (SELECT paciente_id FROM pacientes_da_carga)
   OR usuario_id IN (SELECT usuario_id FROM pacientes_da_carga);
DELETE FROM paciente WHERE id IN (SELECT paciente_id FROM pacientes_da_carga);
DELETE FROM usuario WHERE id IN (SELECT usuario_id FROM pacientes_da_carga);

-- Serviço do teste: só sai se não sobrou senha de outro paciente nele.
DELETE FROM configuracao_servico
WHERE servico_id IN (SELECT id FROM servico WHERE nome = 'Teste de Carga')
  AND NOT EXISTS (SELECT 1 FROM senha s JOIN servico v ON v.id = s.servico_id WHERE v.nome = 'Teste de Carga');
DELETE FROM servico
WHERE nome = 'Teste de Carga'
  AND NOT EXISTS (SELECT 1 FROM senha s JOIN servico v ON v.id = s.servico_id WHERE v.nome = 'Teste de Carga');

COMMIT;
