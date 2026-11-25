-- =====================================================================
-- FilaSUS - conferência do teste de carga do pico de entrada (SCRUM-62)
-- Driver DA-DES-I2-01 · rodar depois do k6, no banco filasus:
--     psql -U filasus -d filasus -f docs/qa/carga/verificar-senhas-repetidas.sql
-- =====================================================================

-- 1. Senhas repetidas no mesmo serviço e dia. Resultado esperado: nenhuma linha.
SELECT servico_id, data_ref, numero, COUNT(*) AS vezes
FROM senha
GROUP BY servico_id, data_ref, numero
HAVING COUNT(*) > 1;

-- 2. Senhas geradas hoje para os pacientes do teste. Resultado esperado: 300.
SELECT COUNT(*) AS senhas_do_teste
FROM senha s
JOIN paciente p ON p.id = s.paciente_id
WHERE p.email LIKE 'carga.%@exemplo.test'
  AND s.data_ref = CURRENT_DATE;

-- 3. Paciente do teste com mais de uma senha no mesmo serviço e dia. Esperado: nenhuma linha.
SELECT s.paciente_id, s.servico_id, COUNT(*) AS senhas
FROM senha s
JOIN paciente p ON p.id = s.paciente_id
WHERE p.email LIKE 'carga.%@exemplo.test'
GROUP BY s.paciente_id, s.servico_id, s.data_ref
HAVING COUNT(*) > 1;

-- 4. Faixa de números gerados no teste (de 1 em 1, sem buraco esperado só se a fila começou vazia).
SELECT s.servico_id, MIN(s.numero) AS primeiro, MAX(s.numero) AS ultimo, COUNT(DISTINCT s.numero) AS distintos
FROM senha s
JOIN paciente p ON p.id = s.paciente_id
WHERE p.email LIKE 'carga.%@exemplo.test'
  AND s.data_ref = CURRENT_DATE
GROUP BY s.servico_id;
