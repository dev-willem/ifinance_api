-- Usuários autenticados via OAuth2.
-- Nunca armazena senha — subject_id é o identificador do provedor.
CREATE TABLE users (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id  VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    provider    VARCHAR(50)  NOT NULL, -- google, github, etc.
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_subject_id UNIQUE (subject_id),
    CONSTRAINT uq_users_email      UNIQUE (email)
);

CREATE INDEX idx_users_subject_id ON users (subject_id);
CREATE INDEX idx_users_email      ON users (email);
