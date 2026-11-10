-- Learning Score — esquema inicial (PostgreSQL)
-- Convenção: toda tabela tem id, criado_em e atualizado_em.
-- Tabelas de propriedade do estudante usam ON DELETE CASCADE (exclusão de conta, RF12).

CREATE TABLE usuario (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(120) NOT NULL,
    email          VARCHAR(180) NOT NULL UNIQUE,
    senha_hash     VARCHAR(100) NOT NULL,
    papel          VARCHAR(20)  NOT NULL DEFAULT 'ESTUDANTE' CHECK (papel IN ('ESTUDANTE','PROFESSOR')),
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE disciplina (
    id                     BIGSERIAL PRIMARY KEY,
    usuario_id             BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    nome                   VARCHAR(120) NOT NULL,
    professor              VARCHAR(120),
    palavras_chave         VARCHAR(500) NOT NULL DEFAULT '',
    frequencia_percentual  NUMERIC(5,2) CHECK (frequencia_percentual BETWEEN 0 AND 100),
    criado_em              TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em          TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_disciplina_usuario ON disciplina(usuario_id);

CREATE TABLE nota (
    id              BIGSERIAL PRIMARY KEY,
    disciplina_id   BIGINT       NOT NULL REFERENCES disciplina(id) ON DELETE CASCADE,
    descricao       VARCHAR(120) NOT NULL,
    valor           NUMERIC(4,2) NOT NULL CHECK (valor BETWEEN 0 AND 10),
    peso            NUMERIC(4,2) NOT NULL DEFAULT 1 CHECK (peso > 0),
    data_lancamento DATE         NOT NULL,
    criado_em       TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_nota_disciplina ON nota(disciplina_id);

CREATE TABLE atividade (
    id            BIGSERIAL PRIMARY KEY,
    usuario_id    BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    disciplina_id BIGINT       REFERENCES disciplina(id) ON DELETE SET NULL,
    titulo        VARCHAR(160) NOT NULL,
    descricao     VARCHAR(2000),
    prazo         TIMESTAMP    NOT NULL,
    prioridade    VARCHAR(10)  NOT NULL DEFAULT 'MEDIA' CHECK (prioridade IN ('BAIXA','MEDIA','ALTA')),
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE','EM_ANDAMENTO','CONCLUIDA','ATRASADA')),
    link_entrega  VARCHAR(500),
    criado_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_atividade_usuario_prazo ON atividade(usuario_id, prazo);
CREATE INDEX idx_atividade_usuario_status ON atividade(usuario_id, status);

CREATE TABLE evento_agenda (
    id            BIGSERIAL PRIMARY KEY,
    usuario_id    BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    titulo        VARCHAR(160) NOT NULL,
    descricao     VARCHAR(2000),
    data          DATE         NOT NULL,
    horario       TIME,
    categoria     VARCHAR(30)  NOT NULL DEFAULT 'COMPROMISSO'
                  CHECK (categoria IN ('PROVA','EVENTO_ACADEMICO','COMPROMISSO','OUTRO')),
    observacoes   VARCHAR(1000),
    criado_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_evento_usuario_data ON evento_agenda(usuario_id, data);

CREATE TABLE notificacao (
    id            BIGSERIAL PRIMARY KEY,
    usuario_id    BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    tipo          VARCHAR(30)  NOT NULL CHECK (tipo IN ('PRAZO_PROXIMO','ATIVIDADE_ATRASADA','EVENTO_FUTURO','ATUALIZACAO_ACADEMICA')),
    titulo        VARCHAR(160) NOT NULL,
    mensagem      VARCHAR(500) NOT NULL,
    lida          BOOLEAN      NOT NULL DEFAULT FALSE,
    chave_unica   VARCHAR(120) NOT NULL,           -- evita notificações duplicadas para o mesmo fato
    criado_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (usuario_id, chave_unica)
);
CREATE INDEX idx_notificacao_usuario ON notificacao(usuario_id, lida);

CREATE TABLE preferencia_notificacao (
    id            BIGSERIAL PRIMARY KEY,
    usuario_id    BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    tipo          VARCHAR(30) NOT NULL CHECK (tipo IN ('PRAZO_PROXIMO','ATIVIDADE_ATRASADA','EVENTO_FUTURO','ATUALIZACAO_ACADEMICA')),
    ativa         BOOLEAN     NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (usuario_id, tipo)
);

CREATE TABLE conteudo (
    id             BIGSERIAL PRIMARY KEY,
    titulo         VARCHAR(160) NOT NULL,
    descricao      VARCHAR(1000),
    url_embed      VARCHAR(300),                    -- apenas o link do vídeo (nunca o arquivo)
    palavras_chave VARCHAR(500) NOT NULL DEFAULT '',
    aprovado       BOOLEAN      NOT NULL DEFAULT FALSE,
    aprovado_por   VARCHAR(120),
    sugerido_por   BIGINT       REFERENCES usuario(id) ON DELETE SET NULL,
    criado_em      TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE recomendacao (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    disciplina_id  BIGINT       NOT NULL REFERENCES disciplina(id) ON DELETE CASCADE,
    conteudo_id    BIGINT       NOT NULL REFERENCES conteudo(id) ON DELETE CASCADE,
    compatibilidade NUMERIC(5,2) NOT NULL CHECK (compatibilidade BETWEEN 80 AND 100),
    criado_em      TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (disciplina_id, conteudo_id)
);
CREATE INDEX idx_recomendacao_usuario ON recomendacao(usuario_id);

CREATE TABLE plataforma_externa (
    id                    BIGSERIAL PRIMARY KEY,
    usuario_id            BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    nome                  VARCHAR(80)  NOT NULL,
    tipo                  VARCHAR(30)  NOT NULL,
    identificador_externo VARCHAR(120),
    simulada              BOOLEAN      NOT NULL DEFAULT TRUE,   -- TRUE = sem integração real
    ativa                 BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em             TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em         TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (usuario_id, nome)
);

CREATE TABLE historico_sincronizacao (
    id            BIGSERIAL PRIMARY KEY,
    usuario_id    BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    plataforma_id BIGINT      NOT NULL REFERENCES plataforma_externa(id) ON DELETE CASCADE,
    tipo          VARCHAR(15) NOT NULL CHECK (tipo IN ('MANUAL','AUTOMATICA')),
    status        VARCHAR(15) NOT NULL CHECK (status IN ('SUCESSO','FALHA','TIMEOUT')),
    mensagem      VARCHAR(300),
    iniciado_em   TIMESTAMP   NOT NULL,
    finalizado_em TIMESTAMP,
    criado_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_sync_usuario_tipo ON historico_sincronizacao(usuario_id, tipo, iniciado_em DESC);

CREATE TABLE consentimento (
    id            BIGSERIAL PRIMARY KEY,
    usuario_id    BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    tipo          VARCHAR(30) NOT NULL CHECK (tipo IN ('TERMOS_USO','INTEGRACOES','USO_DADOS_EXTERNOS')),
    concedido     BOOLEAN     NOT NULL,
    registrado_em TIMESTAMP   NOT NULL DEFAULT NOW(),
    criado_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (usuario_id, tipo)
);

CREATE TABLE registro_financeiro (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT        NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    descricao      VARCHAR(160)  NOT NULL,
    competencia    DATE          NOT NULL,
    valor          NUMERIC(10,2) NOT NULL CHECK (valor >= 0),
    vencimento     DATE          NOT NULL,
    status         VARCHAR(10)   NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE','PAGO')),
    data_pagamento DATE,
    criado_em      TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_financeiro_usuario ON registro_financeiro(usuario_id, vencimento);

-- Sem FK para usuario: o registro da solicitação deve sobreviver à exclusão da conta (prova de atendimento LGPD).
-- Guarda apenas o hash do e-mail (sem dados pessoais legíveis).
CREATE TABLE solicitacao_exclusao (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT      NOT NULL,
    email_hash     VARCHAR(64) NOT NULL,
    status         VARCHAR(15) NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE','CONCLUIDA','CANCELADA')),
    solicitada_em  TIMESTAMP   NOT NULL DEFAULT NOW(),
    processada_em  TIMESTAMP,
    criado_em      TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_exclusao_status ON solicitacao_exclusao(status, solicitada_em);
