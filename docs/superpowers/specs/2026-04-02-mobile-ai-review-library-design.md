# AI Review Library — Mobile

**Platform:** Android (Workshop06, Kotlin) + Spring Boot backend  
**Status:** Planning

---

## What it does

Use an AI library to assist with review moderation or review quality. Two likely interpretations — clarify before implementing:

**Option A — AI-assisted review moderation:** Automatically flag or pre-screen submitted reviews for spam, offensive content, or low quality before they reach manual review queue.

**Option B — AI-generated review summaries:** Summarise a product's reviews into a short blurb (e.g. "Customers love the flaky texture but note it sells out early").

**Recommendation:** Clarify intent with the team. These are different features with different scopes.

---

## Option A: Moderation (backend-focused)

**Backend:**
- Add a moderation step in `ReviewService.create()` before saving
- Call a text classification API (e.g. OpenAI Moderation API, Perspective API, or on-device ML Kit)
- If flagged: auto-set status to `rejected` with `moderation_flag` reason, or quarantine as `pending` with a flag for manual review
- Add `moderationFlag` field to `Review` model and migration

**Android:**
- No UI change — moderation is transparent to the reviewer
- Optionally show "Your review is under review" messaging if auto-flagged

## Option B: Summaries (display feature)

**Backend:**
- New endpoint: `GET /api/v1/products/{productId}/reviews/summary` → `{ summary: String }`
- Calls Claude/OpenAI API with approved reviews as context, returns cached summary
- Cache invalidated when new reviews are approved

**Android:**
- Product detail screen: show AI summary card above the review list

---

## Open questions

- Which option (A, B, or both)?
- Which AI provider — Claude API, OpenAI, on-device ML Kit?
- Budget/API key availability for the demo?
