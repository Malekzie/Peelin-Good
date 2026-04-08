# Profile Preferences Parity — Mobile

**Platform:** Android (Workshop06, Kotlin)  
**Status:** Planning

---

## What it does

The web frontend has a profile/preferences section that is missing or incomplete on mobile. This brings mobile to full parity with web for profile management.

---

## Scope

Audit what the web profile screen exposes and replicate on mobile:
- Basic info: first name, last name, email, phone
- Preferences: dietary preferences (e.g. `VEGAN`, `GLUTEN_FREE` — from `preference_type` enum)
- Profile photo: upload and view pending/approved state
- Password change (see also `2026-04-02-mobile-forgot-password-design.md`)

---

## Backend

All required endpoints already exist:
- `GET /api/v1/customers/me` — full profile
- `PATCH /api/v1/customers/me` — update fields
- `POST /api/v1/account/profile-photo` — upload photo
- `PUT /api/v1/account/password` — change password

No backend changes needed.

---

## Android changes

**Audit current `ProfileFragment`** — identify which fields are present vs missing.

**Expected screens/sections:**
- `ProfileFragment` — display name, email, phone, profile photo with upload button
- `PreferencesFragment` (or section within profile) — multi-select chips for dietary preferences mapped to `preference_type` enum values
- Photo status badge — "Pending review" / "Approved" / "Upload photo"

**API calls needed:**
- `GET /api/v1/customers/me` on load
- `PATCH /api/v1/customers/me` on save
- `POST /api/v1/account/profile-photo` with multipart form data

---

## Open questions

- What preference types does the `preference_type` enum currently contain? Check `V1__baseline.sql`.
- Is the web preferences UI a reference to copy from, or should mobile have a different layout?
