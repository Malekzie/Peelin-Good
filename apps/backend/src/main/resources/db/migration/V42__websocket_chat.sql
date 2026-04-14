ALTER TABLE chat_thread ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'general';
ALTER TABLE chat_thread ADD COLUMN closed_at TIMESTAMPTZ;

CREATE TABLE staff_conversation (
    conversation_id  SERIAL PRIMARY KEY,
    user_a_id        UUID NOT NULL REFERENCES "user"(user_id),
    user_b_id        UUID NOT NULL REFERENCES "user"(user_id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_a_id, user_b_id)
);

CREATE TABLE staff_message (
    message_id       SERIAL PRIMARY KEY,
    conversation_id  INTEGER NOT NULL REFERENCES staff_conversation(conversation_id),
    sender_id        UUID NOT NULL REFERENCES "user"(user_id),
    message_text     VARCHAR(2000),
    sent_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_read          BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_staff_message_convo ON staff_message(conversation_id, sent_at);
