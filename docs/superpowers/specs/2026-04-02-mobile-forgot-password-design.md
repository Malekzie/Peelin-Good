# Forgot Password — Mobile

**Platform:** Android (Workshop06, Kotlin) + Spring Boot backend  
**Status:** Planning

---

## What it does

Add a "Forgot password?" flow to the mobile login screen. User enters their email, receives a reset link or OTP, and can set a new password.

---

## Scope

- "Forgot password?" link on login screen
- User enters email → backend sends reset email
- Reset via link (token in URL) or OTP code — see open questions
- New password set, user redirected to login

---

## Backend changes

**New endpoints:**
- `POST /api/v1/auth/forgot-password` — accepts `{ email }`, sends reset email, returns 204
- `POST /api/v1/auth/reset-password` — accepts `{ token, newPassword }`, validates token, updates password

**New model/migration:**
```sql
CREATE TABLE password_reset_tokens (
    token       VARCHAR(255) PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(user_id),
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE
);
```

**Email sending:**
- Spring Mail (`spring-boot-starter-mail`) with SMTP config
- Token: `UUID.randomUUID()`, expires in 1 hour
- Reset URL format: `https://app.domain/reset-password?token={token}` (deep link for mobile)

**Environment variables:**
- `SPRING_MAIL_HOST`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`

---

## Android changes

- Login screen: "Forgot password?" text button
- `ForgotPasswordFragment` — email input, submit button, success state
- `ResetPasswordFragment` — receives token via deep link intent, new password + confirm fields
- Deep link intent filter in `AndroidManifest.xml` for the reset URL scheme

---

## Open questions

- OTP code vs reset link? Link is simpler to implement; OTP avoids opening a browser.
- Which email provider for the demo — Gmail SMTP, SendGrid, Resend?
- Is this also needed on web? (Not listed in meeting notes but logically consistent)
