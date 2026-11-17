-- Avaliações existentes passam a valer 10, preservando suas notas.
ALTER TABLE nota ADD COLUMN valor_avaliacao NUMERIC(4,2) NOT NULL DEFAULT 10;
ALTER TABLE nota ADD CONSTRAINT ck_nota_valor_avaliacao CHECK (valor_avaliacao > 0 AND valor_avaliacao <= 10);
ALTER TABLE nota ADD CONSTRAINT ck_nota_valor_obtido CHECK (valor <= valor_avaliacao);
