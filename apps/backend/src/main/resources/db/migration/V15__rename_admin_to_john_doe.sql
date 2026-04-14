-- V15: Rename seeded admin user for easier remembering.
-- Updates only the auth identifiers (username + email) for the seeded admin row.

UPDATE "user"
SET username = 'john.doe',
    user_email = 'john.doe@northharbourmail.ca'
WHERE user_id = '10000000-0000-4000-8000-000000000001'::uuid;

