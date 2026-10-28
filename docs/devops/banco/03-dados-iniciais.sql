-- =====================================================================
-- FilaSUS - 03: dados iniciais (fictícios)
-- SCRUM-70 · PostgreSQL 16
--
-- Rodar depois do 02, conectado no banco filasus com o usuário filasus:
--     psql -U filasus -d filasus -f docs/devops/banco/03-dados-iniciais.sql
--
-- Cria os três serviços da US-06 com uma configuração padrão. O gestor
-- muda horário e limite pela tela de configuração (US-09).
-- Pode rodar de novo sem duplicar nada.
-- =====================================================================

BEGIN;

INSERT INTO servico (nome) VALUES
    ('Clínico'),
    ('Enfermagem'),
    ('Odontologia')
ON CONFLICT (nome) DO NOTHING;

INSERT INTO configuracao_servico (servico_id, hora_abertura, hora_fechamento, limite_senhas_dia)
SELECT s.id, TIME '07:00', TIME '17:00', c.limite
FROM servico s
JOIN (VALUES ('Clínico', 40), ('Enfermagem', 60), ('Odontologia', 20)) AS c (nome, limite)
    ON c.nome = s.nome
ON CONFLICT (servico_id) DO NOTHING;

COMMIT;
