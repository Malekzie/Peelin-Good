-- Idempotency table for Stripe webhook events. Stripe will retry the same event with the
-- same id; the StripeWebhookController consults this table before fulfilling so a replay
-- (intentional or accidental) cannot double-credit rewards or re-send confirmation emails.
CREATE TABLE stripe_processed_event (
    event_id      VARCHAR(255) PRIMARY KEY,
    processed_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
