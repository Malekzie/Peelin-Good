-- One OAuth identity per provider (password accounts keep provider / provider_id null)
CREATE UNIQUE INDEX IF NOT EXISTS user_provider_provider_id_key
    ON "user" (provider, provider_id)
    WHERE provider IS NOT NULL AND provider_id IS NOT NULL;
