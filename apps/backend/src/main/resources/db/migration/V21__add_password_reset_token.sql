CREATE TABLE password_reset_token (
                                      id          BIGSERIAL PRIMARY KEY,
                                      user_id     UUID NOT NULL REFERENCES "user"(user_id) ON DELETE CASCADE,
                                      token       VARCHAR(255) NOT NULL UNIQUE,
                                      expires_at  TIMESTAMPTZ NOT NULL,
                                      used        BOOLEAN NOT NULL DEFAULT FALSE,
                                      created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_token_token ON password_reset_token(token);
CREATE INDEX idx_password_reset_token_user_id ON password_reset_token(user_id);