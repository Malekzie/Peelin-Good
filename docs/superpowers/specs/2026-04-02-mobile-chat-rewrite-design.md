# Chat Rewrite — Mobile

**Platform:** Android (Workshop06, Kotlin) + Spring Boot backend  
**Status:** Planning

---

## What it does

Strip out the existing chat implementation on mobile and replace it with a clean, working version. The current implementation is considered broken/unmaintainable.

---

## Scope

- Remove all existing mobile chat code (UI, ViewModels, repositories, network calls)
- Rebuild: customer can open a support thread and exchange messages with staff
- Real-time or polling-based (see open questions)
- Staff side (web) is out of scope for this spec — backend WebSocket/REST endpoints already exist

---

## Existing backend (do not change)

The backend chat REST API is fully functional:

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/chat/threads/me/open` | Get or create customer's open thread |
| `GET /api/v1/chat/threads/{id}/messages` | List messages |
| `POST /api/v1/chat/threads/{id}/messages` | Send message |
| `POST /api/v1/chat/threads/{id}/read` | Mark as read |

WebSocket endpoint may also exist — verify before implementing polling fallback.

---

## Android — what to delete

- Any existing `Chat*` Activity/Fragment/ViewModel/Repository files
- Any existing WebSocket client code (likely broken)
- Remove chat nav entry if currently visible

---

## Android — what to build

**Data layer:**
- `ChatRepository` — Retrofit calls to the endpoints above
- `ChatMessage` data class matching `ChatMessageDto`

**UI:**
- `ChatFragment` — message list (RecyclerView) + text input
- `ChatViewModel` — holds message list, handles send, polling or WebSocket
- Add chat entry to bottom nav (customer-only)

**Polling strategy (if no working WebSocket):**
- Poll `GET messages` every 5 seconds while chat screen is active
- `POST /read` on screen focus

---

## Open questions

- WebSocket vs polling: does the backend have a working WebSocket endpoint? Check `ChatWebSocketController` or similar.
- Should unread message count badge appear on the nav icon?
- Staff-to-customer push notifications out of scope?
