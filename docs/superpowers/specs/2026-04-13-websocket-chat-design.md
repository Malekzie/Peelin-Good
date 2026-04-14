# WebSocket Chat Implementation Design

**Date:** 2026-04-13
**Scope:** Full-stack WebSocket integration for real-time chat across Workshop7 (backend + SvelteKit), Workshop06 (Android), and Workshop5 (JavaFX desktop)

---

## Overview

Two separate chat systems sharing one WebSocket infrastructure:

1. **Customer-to-staff support chat** (`/api/v1/chat/*`) — backend already exists (ChatService, ChatRestController, ChatThread, ChatMessage). Needs: WebSocket layer, category filtering, thread close, frontend UI, Android adaptation.
2. **Staff-to-staff direct messaging** (`/api/v1/messages/*`) — fully new. REST endpoints, service layer, DB tables, all three clients.

**Transport model:** REST + WebSocket hybrid. REST is the primary data path for all CRUD. WebSocket pushes three real-time signals: new message notifications, typing indicators, and read receipts. If WS disconnects, chat degrades gracefully to REST-only.

**Broker:** Spring's built-in simple STOMP broker. No external messaging dependencies. Single backend instance — no need for Redis pub/sub bridging.

---

## Architecture

```
                    Clients
    ┌───────────┬──────────────┬──────────────┐
    │ SvelteKit │   Android    │   Desktop    │
    │ (browser) │  (OkHttp)    │  (JavaFX)    │
    └─────┬─────┴──────┬───────┴──────┬───────┘
          │            │              │
    ┌─────┴────────────┴──────────────┴───────┐
    │           Spring Boot Backend            │
    │                                          │
    │  REST Layer           WebSocket Layer     │
    │  ┌─────────────────┐  ┌────────────────┐ │
    │  │ChatRestController│  │ STOMP Broker   │ │
    │  │/api/v1/chat/*   │  │ /ws endpoint   │ │
    │  ├─────────────────┤  ├────────────────┤ │
    │  │StaffMsgController│  │ChatWsController│ │
    │  │/api/v1/messages/*│  │StaffMsgWsCtrl  │ │
    │  └────────┬────────┘  └───────┬────────┘ │
    │           │                   │          │
    │           ▼                   ▼          │
    │  ┌──────────────────────────────────┐    │
    │  │  Shared: StompChannelInterceptor │    │
    │  │  (JWT auth on CONNECT frames)    │    │
    │  └──────────────────────────────────┘    │
    │                                          │
    │  Services: ChatService (existing),       │
    │            StaffMessageService (new)      │
    │  Cache: Redis (@Cacheable, existing)     │
    │  DB: PostgreSQL + Flyway                 │
    └──────────────────────────────────────────┘
```

---

## WebSocket Infrastructure

### Connection

- Endpoint: `/ws` via SockJS fallback
- Auth: JWT extracted from cookie (web) or STOMP `Authorization` header (mobile/desktop)
- Protocol: STOMP over WebSocket

### StompChannelInterceptor

Intercepts STOMP `CONNECT` frames:
1. Extract JWT from STOMP header or upgrade request cookie
2. Validate via `JwtService`
3. Check `TokenDenylistService`
4. Set `SecurityContextHolder` principal on WS session
5. Reject with STOMP `ERROR` frame if invalid

### Topic Structure

**Customer-to-staff chat:**
```
/topic/chat/thread/{threadId}/messages    — new messages
/topic/chat/thread/{threadId}/typing      — typing indicators
/topic/chat/thread/{threadId}/read        — read receipts
```

**Staff-to-staff messaging:**
```
/topic/messages/conversation/{convoId}/messages
/topic/messages/conversation/{convoId}/typing
/topic/messages/conversation/{convoId}/read
```

**User-specific notifications:**
```
/user/queue/chat/notifications       — new thread assigned, thread closed
/user/queue/messages/notifications   — new DM from staff member
```

### Broker Configuration

```
/topic  — broadcast destinations
/user   — user-specific destinations
/app    — application-routed (client → server)
```

### SecurityConfig Change

Add `/ws/**` to `permitAll()` in HTTP filter chain. Auth handled at STOMP level by `StompChannelInterceptor`.

---

## Database Changes

### Flyway Migration

Single migration file following existing naming convention.

**Alter `chat_thread` (existing table):**

```sql
ALTER TABLE chat_thread ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'general';
ALTER TABLE chat_thread ADD COLUMN closed_at TIMESTAMPTZ;
```

**New tables for staff-to-staff:**

```sql
CREATE TABLE staff_conversation (
    conversation_id  SERIAL PRIMARY KEY,
    user_a_id        UUID NOT NULL REFERENCES app_user(user_id),
    user_b_id        UUID NOT NULL REFERENCES app_user(user_id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_a_id, user_b_id)
);

CREATE TABLE staff_message (
    message_id       SERIAL PRIMARY KEY,
    conversation_id  INTEGER NOT NULL REFERENCES staff_conversation(conversation_id),
    sender_id        UUID NOT NULL REFERENCES app_user(user_id),
    message_text     VARCHAR(2000),
    sent_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_read          BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_staff_message_convo ON staff_message(conversation_id, sent_at);
```

**Design constraint:** `user_a_id < user_b_id` enforced in service layer (lexicographic UUID ordering) to guarantee the unique constraint works.

---

## Backend Service Layer

### Customer-to-Staff (extend existing `ChatService`)

New methods:
- `createThread(category)` — overload accepting category from pre-chat prompt
- `closeThread(threadId)` — sets status to `closed`, stamps `closed_at`, pushes WS notification
- `openThreads(category)` — filtered variant for staff inbox

Existing methods unchanged: `postMessage`, `messages`, `assignEmployee`, `markRead`. After `postMessage` succeeds, push message DTO to STOMP topic via `SimpMessagingTemplate`. Same for `markRead`.

### Staff-to-Staff (new `StaffMessageService`)

- `getOrCreateConversation(otherUserId)` — find or create, enforce UUID ordering
- `conversations()` — list for current user, ordered by `updated_at`
- `messages(conversationId)` — fetch history, permission-checked
- `sendMessage(conversationId, text)` — persist + push via WS
- `markRead(conversationId)` — update DB + push read receipt

### Typing Indicators (shared)

Pure WebSocket relay — no DB, no service logic:
- Client sends STOMP `SEND` to `/app/chat/thread/{id}/typing` or `/app/messages/conversation/{id}/typing`
- `@MessageMapping` handler rebroadcasts to the topic
- Payload: `{ "userId": "uuid", "typing": true }`

### Redis Caching

Staff message caches follow existing pattern: `@Cacheable` on reads, `@CacheEvict` on writes. Short TTL (15-30s). WS push is primary delivery; cache serves initial loads and reconnection.

---

## REST Endpoints

### Customer-to-Staff (additions to existing `ChatRestController`)

| Method | Path | Change | Description |
|--------|------|--------|-------------|
| `POST` | `/api/v1/chat/threads` | Modified | Accepts `{ "category": "order_issue" }` |
| `GET` | `/api/v1/chat/threads` | Modified | Optional `?category=` filter param |
| `POST` | `/api/v1/chat/threads/{threadId}/close` | New | Staff closes/resolves thread |

All other existing endpoints unchanged.

### Staff-to-Staff (new `StaffMessageController`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/messages/conversations` | List current user's conversations |
| `POST` | `/api/v1/messages/conversations` | Start conversation `{ "recipientId": "uuid" }` |
| `GET` | `/api/v1/messages/conversations/{convoId}/messages` | Message history |
| `POST` | `/api/v1/messages/conversations/{convoId}/messages` | Send message `{ "text": "..." }` |
| `POST` | `/api/v1/messages/conversations/{convoId}/read` | Mark as read |

**Security:** All staff-to-staff endpoints require `ADMIN` or `EMPLOYEE` role. Staff DMs are private — no admin override to read other staff conversations.

---

## WebSocket Message Controllers

### ChatWebSocketController

```
@MessageMapping("/chat/thread/{threadId}/typing")
→ Validate access → broadcast to /topic/chat/thread/{threadId}/typing

@MessageMapping("/chat/thread/{threadId}/read")
→ Delegate to ChatService.markRead() → broadcast to /topic/chat/thread/{threadId}/read
```

### StaffMessageWebSocketController

```
@MessageMapping("/messages/conversation/{convoId}/typing")
→ Validate participant → broadcast to /topic/messages/conversation/{convoId}/typing

@MessageMapping("/messages/conversation/{convoId}/read")
→ Delegate to StaffMessageService.markRead() → broadcast to /topic/messages/conversation/{convoId}/read
```

**Messages are sent via REST, not WebSocket.** REST handles persistence, validation, cache eviction, and returns proper HTTP status codes. After successful REST send, `SimpMessagingTemplate.convertAndSend()` pushes the message to WS subscribers.

---

## Frontend — SvelteKit

### Floating Chat Widget (Customer)

- Persistent component in root layout, visible when logged in as customer
- Bottom-right bubble icon → expands to overlay/drawer (not a route)
- First open: category picker ("Order Issue", "General Support", "Account Help", "Feedback")
- After selection: `POST /api/v1/chat/threads` with category → show thread
- Existing open thread: skip picker, load via `GET /api/v1/chat/threads/me/open`
- Subscribes to STOMP topics for active thread

### Staff Chat Inbox

- Route under staff dashboard area
- Left panel: thread list, filterable by category
- Right panel: active conversation
- Shows customer name, category badge, assigned employee
- Unassigned threads visually distinct — click to assign
- Close/resolve button per thread

### WebSocket Connection Service (`$lib/services/ws.ts`)

- Connect on login, disconnect on logout
- `@stomp/stompjs` with SockJS fallback
- JWT via cookie (browser automatic)
- Exposes subscribe/unsubscribe functions
- Built-in reconnect with configurable backoff

### Typing Indicators

- Debounced: send typing event on keypress, stop after 2s inactivity
- Display "User is typing..." in chat panel
- Ephemeral — no persistence

### State Management

- Svelte 5 runes (`$state`, `$derived`)
- Messages as `$state` array, WS pushes appended
- Typing indicator as `$state` boolean with timer reset

---

## Mobile — Android (Workshop06)

### UI Changes

Existing `ChatActivity`, `ChatMessageAdapter`, `StaffChatInboxFragment`, `StaffThreadAdapter` stay.

- Add category picker screen/dialog before `ChatActivity` (skip if open thread exists)
- `StaffChatInboxFragment` adds category filter

### ChatApiService Changes

- Add `category` field to thread creation
- Add close thread endpoint

### New: ChatWebSocketClient

- OkHttp WebSocket + lightweight STOMP framing
- JWT as STOMP `Authorization` header on `CONNECT`
- Auto-reconnect with exponential backoff

### Replace Polling Loop

- Remove `handler.postDelayed(refreshRunnable, 1500)` from `ChatActivity`
- Subscribe to `/topic/chat/thread/{threadId}/messages` on thread open
- Push incoming messages directly into adapter
- Keep `loadMessages()` for initial load and WS-disconnect fallback

---

## Desktop — Workshop5 (JavaFX)

### REST Client Rewrite (`MessageApi`)

- Rewrite to call `/api/v1/messages/conversations/*`
- Drop subject field from send flow
- Map responses to existing `ConversationSummary` / `Message` models

### UI Changes (`MessagingController`)

- Remove subject `TextField` from compose area
- Add "Support Chat" sidebar nav item for customer-to-staff thread management
- Staff inbox view: thread list filtered by category, click to view/respond

### Real-Time: Polling (not WebSocket)

- 5-10 second polling timer for new messages
- Manual refresh button stays
- WebSocket for desktop is a future enhancement — staff-only, low urgency

---

## Error Handling & Edge Cases

### WebSocket Disconnection

- All clients treat WS as enhancement. If dropped, REST still works.
- On reconnect: re-subscribe to active topics, one REST fetch to catch missed messages.
- `@stomp/stompjs` and OkHttp handle reconnect with configurable backoff.

### Message Ordering & Deduplication

- Messages ordered by `sent_at` (server-stamped).
- If WS push arrives before/alongside REST response, client deduplicates by `message_id`.

### Thread Lifecycle

- Message to closed thread → 400, frontend shows "Thread closed, start new conversation"
- Staff closes thread while customer typing → WS thread-closed event, input disables
- No active thread → category picker shown
- Existing open thread → skip picker, load thread

### Auth Edge Cases

- JWT expires during WS session → REST calls fail 401 first. Client disconnects WS, redirects to login.
- Token on denylist → same flow. REST is the canary.
- Stale WS can only receive pushes, not mutate state — acceptable.

### Privacy

- Staff DMs are private. No admin override.
- Customer threads: customer sees own only, staff sees assigned + unassigned, admin sees all.

---

## Dependencies

### New (backend)

- `spring-boot-starter-websocket` — Spring's WebSocket + STOMP support

### New (frontend)

- `@stomp/stompjs` — STOMP client for browser (~15KB)

### New (Android)

- Lightweight STOMP library or hand-rolled STOMP framing over OkHttp WebSocket

### Existing (no changes)

- Spring Security, JwtService, TokenDenylistService — reused in StompChannelInterceptor
- Redis caching — extended to staff message caches
- PostgreSQL + Flyway — new migration
- OkHttp (Android) — already in project
