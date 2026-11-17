ALTER TABLE conteudo
    ADD COLUMN url_video VARCHAR(300),
    ADD COLUMN email_professor VARCHAR(180),
    ADD COLUMN status_avaliacao VARCHAR(16) NOT NULL DEFAULT 'PENDENTE',
    ADD COLUMN token_hash VARCHAR(64) UNIQUE,
    ADD COLUMN token_expira_em TIMESTAMPTZ,
    ADD COLUMN convite_em TIMESTAMPTZ,
    ADD COLUMN respondido_em TIMESTAMPTZ,
    ADD COLUMN justificativa VARCHAR(4000),
    ADD COLUMN email_convite_enviado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_resultado_enviado BOOLEAN NOT NULL DEFAULT FALSE;
-- Preserva o catálogo previamente aprovado e não inventa respostas para registros antigos.
UPDATE conteudo SET status_avaliacao = 'APROVADO' WHERE aprovado;
ALTER TABLE conteudo ADD CONSTRAINT ck_conteudo_status
    CHECK (status_avaliacao IN ('PENDENTE', 'APROVADO', 'RECUSADO'));
ALTER TABLE conteudo ADD CONSTRAINT ck_conteudo_aprovacao
    CHECK (aprovado = (status_avaliacao = 'APROVADO'));
CREATE INDEX idx_conteudo_sugestoes ON conteudo(sugerido_por, criado_em DESC);
