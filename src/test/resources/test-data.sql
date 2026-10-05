-- Shadows main data.sql so no demo rows leak into tests.
-- A single harmless statement: a literally empty script fails script parsing.
SELECT 1;
