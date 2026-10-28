-- Controle de acesso por perfil: todo usuário existente passa a ser USER.
ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER',
    ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'));

-- Identidades OAuth2 adicionais vinculadas a um usuário existente (ex.: GitHub ligado
-- a uma conta criada via Google pelo mesmo e-mail verificado). A identidade de origem
-- continua em users.subject_id, então usuários existentes não mudam.
CREATE TABLE user_identities (
    subject_id  VARCHAR(255) PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider    VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_identities_user_id ON user_identities (user_id);
