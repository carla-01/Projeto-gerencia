-- =====================================================================
-- FilaSUS - 01: usuário e banco do ambiente de desenvolvimento
-- SCRUM-70 · PostgreSQL 16
--
-- Rodar UMA vez, conectado como "postgres" (superusuário):
--     psql -U postgres -f docs/devops/banco/01-criar-usuario-e-banco.sql
--
-- A senha abaixo é só do ambiente local (mesmo padrão do
-- application.properties). Nunca usar em homologação ou produção.
-- =====================================================================

CREATE USER filasus WITH PASSWORD 'filasus_dev';

CREATE DATABASE filasus
    OWNER filasus
    ENCODING 'UTF8'
    TEMPLATE template0;

-- Fuso da UBS (Paraíba). Faz o CURRENT_DATE de senha.data_ref virar o dia
-- certo da fila, e não o dia em UTC (que muda às 21h).
ALTER DATABASE filasus SET timezone TO 'America/Fortaleza';
