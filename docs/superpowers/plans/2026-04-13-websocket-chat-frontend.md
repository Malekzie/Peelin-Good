# WebSocket Chat UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add real-time chat UI to the SvelteKit frontend — a floating customer support widget and two staff pages (customer thread inbox + staff-to-staff DMs).

**Architecture:** REST is the primary data path; WebSocket pushes new messages, typing indicators, and read receipts. STOMP over native WebSocket proxied via Vite. Customer widget mounts in the root layout (customer role only). Staff pages live under the existing `/staff/` route group (auth already guarded by `staff/+layout.server.ts`).

**Tech Stack:** SvelteKit 5, Svelte 5 runes, `@stomp/stompjs` v7, Tailwind CSS v4, TypeScript, `@lucide/svelte`

---

## File Map

**New files:**
- `src/lib/services/ws.ts` — STOMP client singleton (connect, disconnect, subscribe, publish)
- `src/lib/services/chat.ts` — REST calls for customer/staff support chat
- `src/lib/services/staff-messages.ts` — REST calls for staff-to-staff DMs
- `src/lib/components/chat/ChatMessageList.svelte` — scrollable message list (shared)
- `src/lib/components/chat/ChatComposer.svelte` — text input + send button (shared)
- `src/lib/components/chat/ChatCategoryPicker.svelte` — category selection step
- `src/lib/components/chat/ChatWidget.svelte` — floating customer bubble + chat overlay
- `src/routes/staff/chat/+page.svelte` — staff customer thread inbox
- `src/routes/staff/messages/+page.svelte` — staff-to-staff DM inbox

**Modified files:**
- `vite.config.ts` — add `/ws` WebSocket proxy
- `src/lib/services/constants.ts` — add `CHAT_API`, `STAFF_MESSAGES_API`
- `src/lib/services/types.ts` — add `ChatThread`, `ChatMessage`, `StaffConversation`, `StaffMessage`
- `src/routes/+layout.svelte` — mount `ChatWidget`, connect/disconnect WS on auth change
- `src/lib/components/staff/StaffSidebar.svelte` — add Chat and Messages nav links

---

### Task 1: Install @stomp/stompjs, add WS proxy, extend constants and types

**Files:**
- Modify: `vite.config.ts`
- Modify: `src/lib/services/constants.ts`
- Modify: `src/lib/services/types.ts`

- [ ] **Step 1: Install the STOMP client library**

Run from `apps/frontend/`:
```bash
npm install @stomp/stompjs
```

- [ ] **Step 2: Add WebSocket proxy to Vite config**

In `vite.config.ts`, add the `/ws` entry to `server.proxy` so the browser can connect through Vite in dev:

```ts
server: {
    proxy: {
        '/api': {
            target: 'http://localhost:8080',
            changeOrigin: true
        },
        '/oauth2': {
            target: 'http://localhost:8080',
            changeOrigin: true
        },
        '/login/oauth2': {
            target: 'http://localhost:8080',
            changeOrigin: true
        },
        '/ws': {
            target: 'http://localhost:8080',
            ws: true
        }
    }
}
```

- [ ] **Step 3: Add API base constants**

Append to `src/lib/services/constants.ts`:
```ts
export const CHAT_API = `${API_V1}/chat` as const;
export const STAFF_MESSAGES_API = `${API_V1}/messages` as const;
```

- [ ] **Step 4: Add chat/message types**

Append to `src/lib/services/types.ts`:
```ts
export interface ChatThread {
    id: number;
    customerUserId: string;
    customerDisplayName: string | null;
    customerUsername: string;
    customerEmail: string | null;
    employeeUserId: string | null;
    status: string;
    category: string;
    createdAt: string;
    updatedAt: string;
    closedAt: string | null;
}

export interface ChatMessage {
    id: number;
    threadId: number;
    senderUserId: string;
    text: string;
    sentAt: string;
    read: boolean;
}

export interface StaffConversation {
    id: number;
    otherUserId: string;
    otherUsername: string;
    updatedAt: string;
    unreadCount: number;
}

export interface StaffMessage {
    id: number;
    conversationId: number;
    senderUserId: string;
    text: string;
    sentAt: string;
    read: boolean;
}

export interface TypingPayload {
    userId: string;
    typing: boolean;
}
```

- [ ] **Step 5: Verify type check passes**

```bash
bun check
```
Expected: no errors.

- [ ] **Step 6: Commit**

```bash
git add apps/frontend/package.json apps/frontend/package-lock.json apps/frontend/vite.config.ts apps/frontend/src/lib/services/constants.ts apps/frontend/src/lib/services/types.ts
git commit -m "feat: add @stomp/stompjs, WS proxy, chat constants and types"
```

---

### Task 2: WebSocket service

**Files:**
- Create: `apps/frontend/src/lib/services/ws.ts`

- [ ] **Step 1: Create ws.ts**

```ts
import { Client, type StompSubscription } from '@stomp/stompjs';
import { browser } from '$app/environment';

let _client: Client | null = null;

interface SubEntry {
    cb: (data: unknown) => void;
    sub: StompSubscription | null;
}

// topic → subscription entry (persisted across reconnects)
const subscriptions = new Map<string, SubEntry>();

function buildClient(): Client {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const brokerURL = `${protocol}//${window.location.host}/ws/websocket`;

    const c = new Client({
        brokerURL,
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000
    });

    c.onConnect = () => {
        for (const [topic, entry] of subscriptions) {
            if (!entry.sub) {
                entry.sub = c.subscribe(topic, (frame) => {
                    try {
                        entry.cb(JSON.parse(frame.body));
                    } catch {
                        entry.cb(frame.body);
                    }
                });
            }
        }
    };

    c.onDisconnect = () => {
        for (const entry of subscriptions.values()) {
            entry.sub = null;
        }
    };

    return c;
}

export function connectWs(): void {
    if (!browser) return;
    if (!_client) _client = buildClient();
    if (!_client.active) _client.activate();
}

export function disconnectWs(): void {
    if (!browser || !_client) return;
    _client.deactivate();
    _client = null;
    subscriptions.clear();
}

/**
 * Subscribe to a STOMP topic. Returns an unsubscribe function.
 * Safe to call before the client connects — subscription is replayed on connect.
 * Calling with the same topic replaces the previous callback.
 */
export function subscribeWs(topic: string, callback: (data: unknown) => void): () => void {
    if (!browser) return () => {};

    const entry: SubEntry = { cb: callback, sub: null };
    subscriptions.set(topic, entry);

    if (_client?.connected) {
        entry.sub = _client.subscribe(topic, (frame) => {
            try {
                entry.cb(JSON.parse(frame.body));
            } catch {
                entry.cb(frame.body);
            }
        });
    }

    return () => {
        entry.sub?.unsubscribe();
        subscriptions.delete(topic);
    };
}

export function publishWs(destination: string, body: unknown): void {
    if (!browser || !_client?.connected) return;
    _client.publish({ destination, body: JSON.stringify(body) });
}
```

- [ ] **Step 2: Verify type check passes**

```bash
bun check
```
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add apps/frontend/src/lib/services/ws.ts
git commit -m "feat: add WebSocket STOMP service"
```

---

### Task 3: Chat REST service

**Files:**
- Create: `apps/frontend/src/lib/services/chat.ts`

- [ ] **Step 1: Create chat.ts**

```ts
import { CHAT_API } from '$lib/services/constants';
import type { ChatMessage, ChatThread } from '$lib/services/types';

/** Customer: returns their open thread(s) (0 or 1). Staff: returns all open threads. */
export async function getThreads(category?: string): Promise<ChatThread[]> {
    const url = category ? `${CHAT_API}/threads?category=${encodeURIComponent(category)}` : `${CHAT_API}/threads`;
    const res = await fetch(url, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch threads');
    return res.json();
}

/** Customer creates a new support thread with the selected category. */
export async function createThread(category: string): Promise<ChatThread> {
    const res = await fetch(`${CHAT_API}/threads`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ category })
    });
    if (!res.ok) throw new Error('Failed to create thread');
    return res.json();
}

export async function getMessages(threadId: number): Promise<ChatMessage[]> {
    const res = await fetch(`${CHAT_API}/threads/${threadId}/messages`, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch messages');
    return res.json();
}

export async function postMessage(threadId: number, text: string): Promise<ChatMessage> {
    const res = await fetch(`${CHAT_API}/threads/${threadId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ text })
    });
    if (!res.ok) throw new Error('Failed to post message');
    return res.json();
}

export async function assignThread(threadId: number): Promise<ChatThread> {
    const res = await fetch(`${CHAT_API}/threads/${threadId}/assign`, {
        method: 'POST',
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to assign thread');
    return res.json();
}

export async function closeThread(threadId: number): Promise<ChatThread> {
    const res = await fetch(`${CHAT_API}/threads/${threadId}/close`, {
        method: 'POST',
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to close thread');
    return res.json();
}

export async function markThreadRead(threadId: number): Promise<void> {
    await fetch(`${CHAT_API}/threads/${threadId}/read`, {
        method: 'POST',
        credentials: 'include'
    });
}
```

- [ ] **Step 2: Verify type check passes**

```bash
bun check
```
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add apps/frontend/src/lib/services/chat.ts
git commit -m "feat: add chat REST service"
```

---

### Task 4: Staff Messages REST service

**Files:**
- Create: `apps/frontend/src/lib/services/staff-messages.ts`

- [ ] **Step 1: Create staff-messages.ts**

```ts
import { STAFF_MESSAGES_API } from '$lib/services/constants';
import type { StaffConversation, StaffMessage } from '$lib/services/types';

export async function getConversations(): Promise<StaffConversation[]> {
    const res = await fetch(`${STAFF_MESSAGES_API}/conversations`, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch conversations');
    return res.json();
}

export async function startConversation(recipientId: string): Promise<StaffConversation> {
    const res = await fetch(`${STAFF_MESSAGES_API}/conversations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ recipientId })
    });
    if (!res.ok) throw new Error('Failed to start conversation');
    return res.json();
}

export async function getConvMessages(convoId: number): Promise<StaffMessage[]> {
    const res = await fetch(`${STAFF_MESSAGES_API}/conversations/${convoId}/messages`, {
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to fetch messages');
    return res.json();
}

export async function sendMessage(convoId: number, text: string): Promise<StaffMessage> {
    const res = await fetch(`${STAFF_MESSAGES_API}/conversations/${convoId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ text })
    });
    if (!res.ok) throw new Error('Failed to send message');
    return res.json();
}

export async function markConvRead(convoId: number): Promise<void> {
    await fetch(`${STAFF_MESSAGES_API}/conversations/${convoId}/read`, {
        method: 'POST',
        credentials: 'include'
    });
}
```

- [ ] **Step 2: Verify type check passes**

```bash
bun check
```
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add apps/frontend/src/lib/services/staff-messages.ts
git commit -m "feat: add staff messages REST service"
```

---

### Task 5: Shared ChatMessageList and ChatComposer components

**Files:**
- Create: `apps/frontend/src/lib/components/chat/ChatMessageList.svelte`
- Create: `apps/frontend/src/lib/components/chat/ChatComposer.svelte`

- [ ] **Step 1: Create ChatMessageList.svelte**

Scrollable message list. Current user's messages on the right in terracotta; other messages on the left. Scrolls to bottom when new messages arrive. Shows a typing indicator row when `typingLabel` is set.

```svelte
<script lang="ts">
    import { tick } from 'svelte';
    import type { ChatMessage, StaffMessage } from '$lib/services/types';

    type Message = ChatMessage | StaffMessage;

    let {
        messages,
        currentUserId,
        typingLabel = ''
    }: {
        messages: Message[];
        currentUserId: string;
        typingLabel?: string;
    } = $props();

    let listEl: HTMLDivElement;

    $effect(() => {
        // Scroll to bottom whenever messages or typing state changes
        messages;
        typingLabel;
        tick().then(() => {
            if (listEl) listEl.scrollTop = listEl.scrollHeight;
        });
    });

    function formatTime(iso: string): string {
        return new Date(iso).toLocaleTimeString('en-CA', {
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    function isMine(msg: Message): boolean {
        if ('senderUserId' in msg) return msg.senderUserId === currentUserId;
        return false;
    }
</script>

<div bind:this={listEl} class="flex flex-1 flex-col gap-2 overflow-y-auto p-3">
    {#each messages as msg (msg.id)}
        {@const mine = isMine(msg)}
        <div class="flex {mine ? 'justify-end' : 'justify-start'}">
            <div class="max-w-[75%]">
                <div
                    class="rounded-2xl px-3 py-2 text-sm {mine
                        ? 'rounded-tr-sm bg-[#C4714A] text-white'
                        : 'rounded-tl-sm bg-white text-[#2C1A0E] shadow-sm'}"
                >
                    {msg.text}
                </div>
                <p class="mt-0.5 px-1 text-[10px] text-[#2C1A0E]/40 {mine ? 'text-right' : 'text-left'}">
                    {formatTime(msg.sentAt)}
                </p>
            </div>
        </div>
    {/each}

    {#if typingLabel}
        <div class="flex justify-start">
            <div class="rounded-2xl rounded-tl-sm bg-white px-3 py-2 text-xs text-[#2C1A0E]/50 shadow-sm">
                {typingLabel} is typing...
            </div>
        </div>
    {/if}

    {#if messages.length === 0 && !typingLabel}
        <div class="flex flex-1 items-center justify-center">
            <p class="text-xs text-[#2C1A0E]/40">No messages yet</p>
        </div>
    {/if}
</div>
```

- [ ] **Step 2: Create ChatComposer.svelte**

Textarea with Enter-to-send (Shift+Enter for newlines). Fires `ontyping` callback on each keystroke for debounced typing indicators. Fires `onsend` with trimmed text on submit.

```svelte
<script lang="ts">
    import { SendHorizontal } from '@lucide/svelte';

    let {
        onsend,
        ontyping,
        disabled = false,
        placeholder = 'Type a message...'
    }: {
        onsend: (text: string) => void;
        ontyping?: () => void;
        disabled?: boolean;
        placeholder?: string;
    } = $props();

    let text = $state('');

    function submit() {
        const trimmed = text.trim();
        if (!trimmed || disabled) return;
        onsend(trimmed);
        text = '';
    }

    function handleKeydown(e: KeyboardEvent) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            submit();
        }
    }
</script>

<div class="flex items-end gap-2 border-t border-[#2C1A0E]/10 bg-[#FAF7F2] p-3">
    <textarea
        bind:value={text}
        oninput={() => ontyping?.()}
        onkeydown={handleKeydown}
        {placeholder}
        {disabled}
        rows="1"
        class="flex-1 resize-none rounded-xl border border-[#2C1A0E]/15 bg-white px-3 py-2 text-sm text-[#2C1A0E] placeholder-[#2C1A0E]/30 outline-none focus:border-[#C4714A] disabled:opacity-50"
    ></textarea>
    <button
        onclick={submit}
        {disabled}
        class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-[#C4714A] text-white transition-colors hover:bg-[#b56340] disabled:opacity-40"
        aria-label="Send message"
    >
        <SendHorizontal class="h-4 w-4" />
    </button>
</div>
```

- [ ] **Step 3: Verify type check passes**

```bash
bun check
```
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add apps/frontend/src/lib/components/chat/
git commit -m "feat: add shared ChatMessageList and ChatComposer components"
```

---

### Task 6: Customer chat widget

**Files:**
- Create: `apps/frontend/src/lib/components/chat/ChatCategoryPicker.svelte`
- Create: `apps/frontend/src/lib/components/chat/ChatWidget.svelte`

- [ ] **Step 1: Create ChatCategoryPicker.svelte**

Shown when a customer opens the widget for the first time (no open thread). Presents 4 category options.

```svelte
<script lang="ts">
    let { onpick }: { onpick: (category: string) => void } = $props();

    const categories = [
        { value: 'general', label: 'General Support' },
        { value: 'order_issue', label: 'Order Issue' },
        { value: 'account_help', label: 'Account Help' },
        { value: 'feedback', label: 'Feedback' }
    ];
</script>

<div class="flex flex-1 flex-col gap-3 p-4">
    <p class="text-sm font-medium text-[#2C1A0E]">How can we help you today?</p>
    <div class="flex flex-col gap-2">
        {#each categories as cat (cat.value)}
            <button
                onclick={() => onpick(cat.value)}
                class="rounded-xl border border-[#2C1A0E]/10 bg-white px-4 py-3 text-left text-sm font-medium text-[#2C1A0E] transition-colors hover:border-[#C4714A] hover:bg-[#C4714A]/5"
            >
                {cat.label}
            </button>
        {/each}
    </div>
</div>
```

- [ ] **Step 2: Create ChatWidget.svelte**

Floating bubble (bottom-right). Visible to customers only. On open, checks for an existing thread; shows category picker if none, loads the thread view if one exists. Subscribes to the thread's STOMP topic when a thread is active.

```svelte
<script lang="ts">
    import { onDestroy } from 'svelte';
    import { MessageCircle, X } from '@lucide/svelte';
    import { user } from '$lib/stores/authStore';
    import { getThreads, createThread, getMessages, postMessage } from '$lib/services/chat';
    import { subscribeWs, publishWs } from '$lib/services/ws';
    import ChatCategoryPicker from './ChatCategoryPicker.svelte';
    import ChatMessageList from './ChatMessageList.svelte';
    import ChatComposer from './ChatComposer.svelte';
    import type { ChatThread, ChatMessage, TypingPayload } from '$lib/services/types';

    const isCustomer = $derived($user?.role === 'customer');

    let open = $state(false);
    let loading = $state(false);
    let thread = $state<ChatThread | null>(null);
    let messages = $state<ChatMessage[]>([]);
    let typingLabel = $state('');
    let typingTimer: ReturnType<typeof setTimeout>;
    let sendError = $state('');

    let unsubMessages: (() => void) | null = null;
    let unsubTyping: (() => void) | null = null;

    function subscribeThread(t: ChatThread) {
        unsubMessages?.();
        unsubTyping?.();

        unsubMessages = subscribeWs(
            `/topic/chat/thread/${t.id}/messages`,
            (data) => {
                const msg = data as ChatMessage;
                // Deduplicate by id in case REST response and WS push overlap
                if (!messages.find((m) => m.id === msg.id)) {
                    messages = [...messages, msg];
                }
            }
        );

        unsubTyping = subscribeWs(
            `/topic/chat/thread/${t.id}/typing`,
            (data) => {
                const payload = data as TypingPayload;
                if (payload.userId === $user?.userId) return;
                typingLabel = 'Agent';
                clearTimeout(typingTimer);
                typingTimer = setTimeout(() => { typingLabel = ''; }, 3000);
            }
        );
    }

    async function handleOpen() {
        open = true;
        if (thread) return;
        loading = true;
        try {
            const threads = await getThreads();
            if (threads.length > 0) {
                thread = threads[0];
                messages = await getMessages(thread.id);
                subscribeThread(thread);
            }
        } catch {
            // No thread yet — category picker shown
        } finally {
            loading = false;
        }
    }

    async function handleCategoryPick(category: string) {
        loading = true;
        try {
            thread = await createThread(category);
            messages = [];
            subscribeThread(thread);
        } catch {
            // Thread creation failed; stay on picker
        } finally {
            loading = false;
        }
    }

    async function handleSend(text: string) {
        if (!thread) return;
        sendError = '';
        try {
            const msg = await postMessage(thread.id, text);
            if (!messages.find((m) => m.id === msg.id)) {
                messages = [...messages, msg];
            }
        } catch {
            sendError = 'Failed to send. Try again.';
        }
    }

    function handleTyping() {
        if (!thread || !$user) return;
        publishWs(`/app/chat/thread/${thread.id}/typing`, {
            userId: $user.userId,
            typing: true
        });
    }

    onDestroy(() => {
        unsubMessages?.();
        unsubTyping?.();
        clearTimeout(typingTimer);
    });
</script>

{#if isCustomer}
    <div class="fixed bottom-6 right-6 z-50">
        {#if open}
            <div
                class="flex h-[480px] w-80 flex-col overflow-hidden rounded-2xl border border-[#2C1A0E]/10 bg-[#FAF7F2] shadow-2xl"
            >
                <!-- Header -->
                <div class="flex shrink-0 items-center justify-between bg-[#2C1A0E] px-4 py-3">
                    <p class="text-sm font-semibold text-[#FAF7F2]">Support Chat</p>
                    <button
                        onclick={() => { open = false; }}
                        class="rounded p-0.5 text-[#FAF7F2]/70 hover:text-[#FAF7F2]"
                        aria-label="Close chat"
                    >
                        <X class="h-4 w-4" />
                    </button>
                </div>

                <!-- Body -->
                {#if loading}
                    <div class="flex flex-1 items-center justify-center">
                        <p class="text-xs text-[#2C1A0E]/40">Loading...</p>
                    </div>
                {:else if thread?.status === 'closed'}
                    <div class="flex flex-1 flex-col">
                        <ChatMessageList {messages} currentUserId={$user?.userId ?? ''} />
                        <div class="border-t border-[#2C1A0E]/10 p-4 text-center">
                            <p class="text-xs text-[#2C1A0E]/50">This conversation is closed.</p>
                            <button
                                onclick={() => { thread = null; messages = []; }}
                                class="mt-2 text-xs font-medium text-[#C4714A] hover:underline"
                            >
                                Start a new conversation
                            </button>
                        </div>
                    </div>
                {:else if !thread}
                    <ChatCategoryPicker onpick={handleCategoryPick} />
                {:else}
                    <ChatMessageList
                        {messages}
                        currentUserId={$user?.userId ?? ''}
                        {typingLabel}
                    />
                    {#if sendError}
                        <p class="px-3 pb-1 text-center text-xs text-red-500">{sendError}</p>
                    {/if}
                    <ChatComposer onsend={handleSend} ontyping={handleTyping} />
                {/if}
            </div>
        {:else}
            <button
                onclick={handleOpen}
                class="flex h-14 w-14 items-center justify-center rounded-full bg-[#C4714A] text-white shadow-lg transition-colors hover:bg-[#b56340]"
                aria-label="Open support chat"
            >
                <MessageCircle class="h-6 w-6" />
            </button>
        {/if}
    </div>
{/if}
```

- [ ] **Step 3: Verify type check passes**

```bash
bun check
```
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add apps/frontend/src/lib/components/chat/
git commit -m "feat: add customer chat widget components"
```

---

### Task 7: Mount ChatWidget and connect WebSocket in root layout

**Files:**
- Modify: `apps/frontend/src/routes/+layout.svelte`

- [ ] **Step 1: Update root layout**

Replace the contents of `+layout.svelte` with:

```svelte
<script lang="ts">
    import './layout.css';
    import { page } from '$app/state';
    import { navigating } from '$app/state';
    import favicon from '$lib/assets/favicon.svg';
    import Navbar from '$lib/components/layout/Navbar.svelte';
    import Footer from '$lib/components/layout/Footer.svelte';
    import ChatWidget from '$lib/components/chat/ChatWidget.svelte';
    import { ModeWatcher } from 'mode-watcher';
    import { user } from '$lib/stores/authStore';
    import { cart, cartCount } from '$lib/stores/cart';
    import { connectWs, disconnectWs } from '$lib/services/ws';

    let { children } = $props();

    const hideFooter = $derived(
        page.url.pathname.startsWith('/staff') || page.url.pathname.startsWith('/profile')
    );

    $effect(() => {
        cart.switchUser($user?.userId ?? null);
    });

    $effect(() => {
        if ($user) {
            connectWs();
        } else {
            disconnectWs();
        }
    });
</script>

<svelte:head><link rel="icon" href={favicon} /></svelte:head>
<ModeWatcher />

{#if navigating}
    <div
        class="fixed top-0 right-0 left-0 z-50 h-0.75 overflow-hidden"
        role="progressbar"
        aria-label="Page loading"
    >
        <div class="animate-nav-progress h-full w-full bg-[#C4714A]"></div>
    </div>
{/if}

<Navbar cartCount={$cartCount} />
{@render children()}
{#if !hideFooter}
    <Footer />
{/if}

<ChatWidget />
```

- [ ] **Step 2: Verify type check passes**

```bash
bun check
```
Expected: no errors.

- [ ] **Step 3: Manual smoke test**

1. Start backend: `mvnw.cmd spring-boot:run` (from `apps/backend/`)
2. Start frontend: `bun dev` (from `apps/frontend/`)
3. Log in as a customer account
4. A terracotta circle button should appear bottom-right
5. Click it — the category picker should appear
6. Pick a category — the chat window should open (empty thread)
7. Type a message and press Enter — it should appear as a sent message
8. Check backend logs for any 4xx/5xx errors

- [ ] **Step 4: Commit**

```bash
git add apps/frontend/src/routes/+layout.svelte
git commit -m "feat: mount ChatWidget and connect WebSocket on auth"
```

---

### Task 8: Staff customer chat inbox

**Files:**
- Create: `apps/frontend/src/routes/staff/chat/+page.svelte`

The page has a split layout: left panel lists open threads with category filter tabs; right panel shows the selected thread's messages plus Assign / Close action buttons.

- [ ] **Step 1: Create the page**

```svelte
<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { user } from '$lib/stores/authStore';
    import {
        getThreads,
        getMessages,
        postMessage,
        assignThread,
        closeThread,
        markThreadRead
    } from '$lib/services/chat';
    import { subscribeWs, publishWs } from '$lib/services/ws';
    import ChatMessageList from '$lib/components/chat/ChatMessageList.svelte';
    import ChatComposer from '$lib/components/chat/ChatComposer.svelte';
    import type { ChatThread, ChatMessage, TypingPayload } from '$lib/services/types';

    const CATEGORIES = [
        { value: '', label: 'All' },
        { value: 'general', label: 'General' },
        { value: 'order_issue', label: 'Order Issue' },
        { value: 'account_help', label: 'Account Help' },
        { value: 'feedback', label: 'Feedback' }
    ];

    let threads = $state<ChatThread[]>([]);
    let selectedThread = $state<ChatThread | null>(null);
    let messages = $state<ChatMessage[]>([]);
    let activeCategory = $state('');
    let loadingThreads = $state(true);
    let loadingMessages = $state(false);
    let typingLabel = $state('');
    let typingTimer: ReturnType<typeof setTimeout>;
    let actionError = $state('');

    let unsubMessages: (() => void) | null = null;
    let unsubTyping: (() => void) | null = null;

    async function loadThreads() {
        loadingThreads = true;
        try {
            threads = await getThreads(activeCategory || undefined);
        } catch {
            // leave threads as-is
        } finally {
            loadingThreads = false;
        }
    }

    async function selectThread(t: ChatThread) {
        selectedThread = t;
        actionError = '';
        loadingMessages = true;

        unsubMessages?.();
        unsubTyping?.();

        try {
            messages = await getMessages(t.id);
            markThreadRead(t.id).catch(() => {});
        } catch {
            messages = [];
        } finally {
            loadingMessages = false;
        }

        unsubMessages = subscribeWs(`/topic/chat/thread/${t.id}/messages`, (data) => {
            const msg = data as ChatMessage;
            if (!messages.find((m) => m.id === msg.id)) {
                messages = [...messages, msg];
            }
        });

        unsubTyping = subscribeWs(`/topic/chat/thread/${t.id}/typing`, (data) => {
            const p = data as TypingPayload;
            if (p.userId === $user?.userId) return;
            typingLabel = 'Customer';
            clearTimeout(typingTimer);
            typingTimer = setTimeout(() => { typingLabel = ''; }, 3000);
        });
    }

    async function handleSend(text: string) {
        if (!selectedThread) return;
        actionError = '';
        try {
            const msg = await postMessage(selectedThread.id, text);
            if (!messages.find((m) => m.id === msg.id)) {
                messages = [...messages, msg];
            }
        } catch {
            actionError = 'Failed to send.';
        }
    }

    function handleTyping() {
        if (!selectedThread || !$user) return;
        publishWs(`/app/chat/thread/${selectedThread.id}/typing`, {
            userId: $user.userId,
            typing: true
        });
    }

    async function handleAssign() {
        if (!selectedThread) return;
        actionError = '';
        try {
            const updated = await assignThread(selectedThread.id);
            selectedThread = updated;
            threads = threads.map((t) => (t.id === updated.id ? updated : t));
        } catch {
            actionError = 'Failed to assign.';
        }
    }

    async function handleClose() {
        if (!selectedThread) return;
        actionError = '';
        try {
            const updated = await closeThread(selectedThread.id);
            threads = threads.filter((t) => t.id !== updated.id);
            selectedThread = null;
            messages = [];
        } catch {
            actionError = 'Failed to close.';
        }
    }

    async function handleCategoryChange(cat: string) {
        activeCategory = cat;
        selectedThread = null;
        messages = [];
        await loadThreads();
    }

    onMount(loadThreads);

    onDestroy(() => {
        unsubMessages?.();
        unsubTyping?.();
        clearTimeout(typingTimer);
    });

    function categoryLabel(cat: string): string {
        return cat.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()) || 'General';
    }

    function formatUpdated(iso: string): string {
        return new Date(iso).toLocaleString('en-CA', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
</script>

<main class="flex flex-1 overflow-hidden">
    <!-- Left panel: thread list -->
    <aside class="flex w-72 shrink-0 flex-col border-r border-border bg-card">
        <div class="border-b border-border p-4">
            <h2 class="text-sm font-semibold text-foreground">Support Inbox</h2>
        </div>

        <!-- Category tabs -->
        <div class="flex gap-1 overflow-x-auto border-b border-border p-2" style="scrollbar-width:none;">
            {#each CATEGORIES as cat (cat.value)}
                <button
                    onclick={() => handleCategoryChange(cat.value)}
                    class="shrink-0 rounded-full px-3 py-1 text-xs font-medium transition-colors {activeCategory === cat.value
                        ? 'bg-[#2C1A0E] text-[#FAF7F2]'
                        : 'text-muted-foreground hover:bg-muted'}"
                >
                    {cat.label}
                </button>
            {/each}
        </div>

        <!-- Thread list -->
        <div class="flex-1 overflow-y-auto">
            {#if loadingThreads}
                <div class="space-y-2 p-3">
                    {#each Array(4) as _, i (i)}
                        <div class="h-16 animate-pulse rounded-lg bg-muted"></div>
                    {/each}
                </div>
            {:else if threads.length === 0}
                <div class="flex flex-1 items-center justify-center p-8">
                    <p class="text-xs text-muted-foreground">No open threads</p>
                </div>
            {:else}
                {#each threads as t (t.id)}
                    <button
                        onclick={() => selectThread(t)}
                        class="w-full border-b border-border px-4 py-3 text-left transition-colors hover:bg-muted {selectedThread?.id === t.id
                            ? 'bg-muted'
                            : ''}"
                    >
                        <div class="flex items-center justify-between gap-2">
                            <p class="truncate text-sm font-medium text-foreground">
                                {t.customerDisplayName ?? t.customerUsername}
                            </p>
                            <span class="shrink-0 rounded-full bg-[#8A9E7F]/20 px-2 py-0.5 text-[10px] font-medium text-[#8A9E7F]">
                                {categoryLabel(t.category)}
                            </span>
                        </div>
                        <p class="mt-0.5 text-xs text-muted-foreground">{formatUpdated(t.updatedAt)}</p>
                        {#if !t.employeeUserId}
                            <p class="mt-0.5 text-[10px] font-medium text-[#C4714A]">Unassigned</p>
                        {/if}
                    </button>
                {/each}
            {/if}
        </div>
    </aside>

    <!-- Right panel: thread detail -->
    <div class="flex flex-1 flex-col overflow-hidden">
        {#if !selectedThread}
            <div class="flex flex-1 items-center justify-center">
                <p class="text-sm text-muted-foreground">Select a thread to view the conversation</p>
            </div>
        {:else}
            <!-- Thread header -->
            <div class="flex shrink-0 items-center justify-between border-b border-border bg-card px-6 py-3">
                <div>
                    <p class="text-sm font-semibold text-foreground">
                        {selectedThread.customerDisplayName ?? selectedThread.customerUsername}
                    </p>
                    <p class="text-xs text-muted-foreground">
                        {categoryLabel(selectedThread.category)}
                        {#if selectedThread.customerEmail}
                            · {selectedThread.customerEmail}
                        {/if}
                    </p>
                </div>
                <div class="flex items-center gap-2">
                    {#if actionError}
                        <p class="text-xs text-destructive">{actionError}</p>
                    {/if}
                    {#if !selectedThread.employeeUserId}
                        <button
                            onclick={handleAssign}
                            class="rounded-lg border border-[#8A9E7F] px-3 py-1.5 text-xs font-medium text-[#8A9E7F] transition-colors hover:bg-[#8A9E7F]/10"
                        >
                            Assign to me
                        </button>
                    {/if}
                    <button
                        onclick={handleClose}
                        class="rounded-lg border border-destructive px-3 py-1.5 text-xs font-medium text-destructive transition-colors hover:bg-destructive/10"
                    >
                        Close thread
                    </button>
                </div>
            </div>

            <!-- Messages -->
            {#if loadingMessages}
                <div class="flex flex-1 items-center justify-center">
                    <p class="text-xs text-muted-foreground">Loading...</p>
                </div>
            {:else}
                <ChatMessageList
                    {messages}
                    currentUserId={$user?.userId ?? ''}
                    {typingLabel}
                />
                <ChatComposer onsend={handleSend} ontyping={handleTyping} />
            {/if}
        {/if}
    </div>
</main>
```

- [ ] **Step 2: Verify type check passes**

```bash
bun check
```
Expected: no errors.

- [ ] **Step 3: Manual smoke test**

1. Log in as staff/admin and navigate to `/staff/chat`
2. The thread list should appear on the left
3. Click a thread — messages load on the right
4. Send a reply — message appears
5. "Assign to me" button visible on unassigned threads; clicking it should update the thread header
6. "Close thread" should remove it from the list

- [ ] **Step 4: Commit**

```bash
git add apps/frontend/src/routes/staff/chat/
git commit -m "feat: add staff customer chat inbox page"
```

---

### Task 9: Staff DM messages page

**Files:**
- Create: `apps/frontend/src/routes/staff/messages/+page.svelte`

Split layout: left panel lists conversations with unread badges; right panel shows the DM thread. A "New Message" button opens an inline staff picker to start a conversation.

- [ ] **Step 1: Create the page**

```svelte
<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { user } from '$lib/stores/authStore';
    import {
        getConversations,
        startConversation,
        getConvMessages,
        sendMessage,
        markConvRead
    } from '$lib/services/staff-messages';
    import { listStaff } from '$lib/services/staff-employees';
    import { subscribeWs, publishWs } from '$lib/services/ws';
    import ChatMessageList from '$lib/components/chat/ChatMessageList.svelte';
    import ChatComposer from '$lib/components/chat/ChatComposer.svelte';
    import type { StaffConversation, StaffMessage, StaffMessage as SM, TypingPayload } from '$lib/services/types';
    import type { UserRecord } from '$lib/services/types';
    import { UserPlus } from '@lucide/svelte';

    let conversations = $state<StaffConversation[]>([]);
    let selectedConvo = $state<StaffConversation | null>(null);
    let messages = $state<StaffMessage[]>([]);
    let loadingConvos = $state(true);
    let loadingMessages = $state(false);
    let typingLabel = $state('');
    let typingTimer: ReturnType<typeof setTimeout>;
    let sendError = $state('');

    // New conversation picker state
    let showPicker = $state(false);
    let staffList = $state<UserRecord[]>([]);
    let staffFilter = $state('');
    let pickerLoading = $state(false);

    let unsubMessages: (() => void) | null = null;
    let unsubTyping: (() => void) | null = null;
    let unsubNotifications: (() => void) | null = null;

    async function loadConversations() {
        loadingConvos = true;
        try {
            conversations = await getConversations();
        } catch {
            // leave as-is
        } finally {
            loadingConvos = false;
        }
    }

    async function selectConvo(c: StaffConversation) {
        selectedConvo = c;
        sendError = '';
        loadingMessages = true;

        unsubMessages?.();
        unsubTyping?.();

        try {
            messages = await getConvMessages(c.id);
            markConvRead(c.id).catch(() => {});
            // Clear unread badge locally
            conversations = conversations.map((cv) =>
                cv.id === c.id ? { ...cv, unreadCount: 0 } : cv
            );
        } catch {
            messages = [];
        } finally {
            loadingMessages = false;
        }

        unsubMessages = subscribeWs(
            `/topic/messages/conversation/${c.id}/messages`,
            (data) => {
                const msg = data as StaffMessage;
                if (!messages.find((m) => m.id === msg.id)) {
                    messages = [...messages, msg];
                }
            }
        );

        unsubTyping = subscribeWs(
            `/topic/messages/conversation/${c.id}/typing`,
            (data) => {
                const p = data as TypingPayload;
                if (p.userId === $user?.userId) return;
                typingLabel = selectedConvo?.otherUsername ?? 'User';
                clearTimeout(typingTimer);
                typingTimer = setTimeout(() => { typingLabel = ''; }, 3000);
            }
        );
    }

    async function handleSend(text: string) {
        if (!selectedConvo) return;
        sendError = '';
        try {
            const msg = await sendMessage(selectedConvo.id, text);
            if (!messages.find((m) => m.id === msg.id)) {
                messages = [...messages, msg];
            }
        } catch {
            sendError = 'Failed to send.';
        }
    }

    function handleTyping() {
        if (!selectedConvo || !$user) return;
        publishWs(`/app/messages/conversation/${selectedConvo.id}/typing`, {
            userId: $user.userId,
            typing: true
        });
    }

    async function openPicker() {
        showPicker = true;
        if (staffList.length > 0) return;
        pickerLoading = true;
        try {
            const all = await listStaff();
            staffList = all.filter((u) => String(u.id) !== String($user?.userId));
        } catch {
            staffList = [];
        } finally {
            pickerLoading = false;
        }
    }

    async function handleStartConversation(staffUser: UserRecord) {
        showPicker = false;
        staffFilter = '';
        try {
            const convo = await startConversation(String(staffUser.id));
            // Add or move to top
            conversations = [convo, ...conversations.filter((c) => c.id !== convo.id)];
            await selectConvo(convo);
        } catch {
            sendError = 'Could not start conversation.';
        }
    }

    const filteredStaff = $derived(
        staffList.filter((u) =>
            u.username.toLowerCase().includes(staffFilter.toLowerCase())
        )
    );

    onMount(async () => {
        await loadConversations();

        // Subscribe to personal notification queue for new DMs
        unsubNotifications = subscribeWs('/user/queue/messages/notifications', (data) => {
            const msg = data as StaffMessage;
            // Increment unread badge for the relevant conversation (if not selected)
            conversations = conversations.map((c) => {
                if (c.id === msg.conversationId && selectedConvo?.id !== c.id) {
                    return { ...c, unreadCount: c.unreadCount + 1 };
                }
                return c;
            });
            // If new convo not in list yet, reload
            if (!conversations.find((c) => c.id === msg.conversationId)) {
                loadConversations();
            }
        });
    });

    onDestroy(() => {
        unsubMessages?.();
        unsubTyping?.();
        unsubNotifications?.();
        clearTimeout(typingTimer);
    });

    function formatUpdated(iso: string): string {
        return new Date(iso).toLocaleString('en-CA', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
</script>

<main class="flex flex-1 overflow-hidden">
    <!-- Left panel -->
    <aside class="flex w-72 shrink-0 flex-col border-r border-border bg-card">
        <div class="flex items-center justify-between border-b border-border p-4">
            <h2 class="text-sm font-semibold text-foreground">Messages</h2>
            <button
                onclick={openPicker}
                class="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
                aria-label="New message"
            >
                <UserPlus class="h-4 w-4" />
            </button>
        </div>

        <!-- New conversation picker -->
        {#if showPicker}
            <div class="border-b border-border bg-muted/30 p-3">
                <input
                    type="text"
                    bind:value={staffFilter}
                    placeholder="Search staff..."
                    class="w-full rounded-lg border border-border bg-white px-3 py-1.5 text-sm outline-none focus:border-[#C4714A]"
                />
                <div class="mt-2 max-h-40 overflow-y-auto">
                    {#if pickerLoading}
                        <p class="py-2 text-center text-xs text-muted-foreground">Loading...</p>
                    {:else if filteredStaff.length === 0}
                        <p class="py-2 text-center text-xs text-muted-foreground">No staff found</p>
                    {:else}
                        {#each filteredStaff as su (su.id)}
                            <button
                                onclick={() => handleStartConversation(su)}
                                class="w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-muted"
                            >
                                {su.username}
                                {#if su.role}<span class="ml-1 text-xs text-muted-foreground capitalize">({su.role})</span>{/if}
                            </button>
                        {/each}
                    {/if}
                </div>
                <button
                    onclick={() => { showPicker = false; staffFilter = ''; }}
                    class="mt-1 w-full text-center text-xs text-muted-foreground hover:text-foreground"
                >
                    Cancel
                </button>
            </div>
        {/if}

        <!-- Conversation list -->
        <div class="flex-1 overflow-y-auto">
            {#if loadingConvos}
                <div class="space-y-2 p-3">
                    {#each Array(3) as _, i (i)}
                        <div class="h-14 animate-pulse rounded-lg bg-muted"></div>
                    {/each}
                </div>
            {:else if conversations.length === 0}
                <div class="flex flex-1 items-center justify-center p-8">
                    <p class="text-xs text-muted-foreground">No conversations yet</p>
                </div>
            {:else}
                {#each conversations as c (c.id)}
                    <button
                        onclick={() => selectConvo(c)}
                        class="w-full border-b border-border px-4 py-3 text-left transition-colors hover:bg-muted {selectedConvo?.id === c.id
                            ? 'bg-muted'
                            : ''}"
                    >
                        <div class="flex items-center justify-between">
                            <p class="text-sm font-medium text-foreground">{c.otherUsername}</p>
                            {#if c.unreadCount > 0}
                                <span class="rounded-full bg-[#C4714A] px-1.5 py-0.5 text-[10px] font-bold text-white">
                                    {c.unreadCount}
                                </span>
                            {/if}
                        </div>
                        <p class="mt-0.5 text-xs text-muted-foreground">{formatUpdated(c.updatedAt)}</p>
                    </button>
                {/each}
            {/if}
        </div>
    </aside>

    <!-- Right panel -->
    <div class="flex flex-1 flex-col overflow-hidden">
        {#if !selectedConvo}
            <div class="flex flex-1 items-center justify-center">
                <p class="text-sm text-muted-foreground">Select a conversation or start a new one</p>
            </div>
        {:else}
            <!-- DM header -->
            <div class="flex shrink-0 items-center border-b border-border bg-card px-6 py-3">
                <div>
                    <p class="text-sm font-semibold text-foreground">{selectedConvo.otherUsername}</p>
                </div>
                {#if sendError}
                    <p class="ml-4 text-xs text-destructive">{sendError}</p>
                {/if}
            </div>

            {#if loadingMessages}
                <div class="flex flex-1 items-center justify-center">
                    <p class="text-xs text-muted-foreground">Loading...</p>
                </div>
            {:else}
                <ChatMessageList
                    {messages}
                    currentUserId={$user?.userId ?? ''}
                    {typingLabel}
                />
                <ChatComposer onsend={handleSend} ontyping={handleTyping} />
            {/if}
        {/if}
    </div>
</main>
```

- [ ] **Step 2: Verify type check passes**

```bash
bun check
```
Expected: no errors.

- [ ] **Step 3: Manual smoke test**

1. Log in as staff and navigate to `/staff/messages`
2. Click the person-plus icon — staff picker should appear
3. Select another staff member — a conversation should open
4. Send a message
5. In a separate browser (incognito), log in as the other staff member and navigate to `/staff/messages` — the conversation should appear with an unread badge
6. Reply — the first browser should receive the message via WebSocket

- [ ] **Step 4: Commit**

```bash
git add apps/frontend/src/routes/staff/messages/
git commit -m "feat: add staff DM messages page"
```

---

### Task 10: Add Chat and Messages to StaffSidebar

**Files:**
- Modify: `apps/frontend/src/lib/components/staff/StaffSidebar.svelte`

- [ ] **Step 1: Add nav entries to the sidebar**

In `StaffSidebar.svelte`, add `MessageCircle` and `MessagesSquare` to the lucide imports and insert two new entries in `allNavLinks`:

Change the import block from:
```ts
import {
    LayoutDashboard,
    ShoppingBag,
    Star,
    Users,
    BarChart2,
    Package,
    UserCog,
    Shield,
    User,
    LogOut
} from '@lucide/svelte';
```
to:
```ts
import {
    LayoutDashboard,
    ShoppingBag,
    Star,
    Users,
    BarChart2,
    Package,
    UserCog,
    Shield,
    User,
    LogOut,
    MessageCircle,
    MessagesSquare
} from '@lucide/svelte';
```

Change `allNavLinks` from:
```ts
const allNavLinks = [
    { label: 'Dashboard', href: '/staff/dashboard', icon: LayoutDashboard, roles: null },
    { label: 'Orders', href: '/staff/orders', icon: ShoppingBag, roles: null },
    { label: 'Customers', href: '/staff/customers', icon: Users, roles: null },
    { label: 'Analytics', href: '/staff/analytics', icon: BarChart2, roles: ['admin'] },
    { label: 'Products', href: '/staff/products', icon: Package, roles: ['admin'] },
    { label: 'Employees', href: '/staff/staff', icon: UserCog, roles: ['admin'] },
    { label: 'Users', href: '/staff/users', icon: Shield, roles: ['admin'] },
    { label: 'My Profile', href: '/staff/profile', icon: User, roles: null }
];
```
to:
```ts
const allNavLinks = [
    { label: 'Dashboard', href: '/staff/dashboard', icon: LayoutDashboard, roles: null },
    { label: 'Orders', href: '/staff/orders', icon: ShoppingBag, roles: null },
    { label: 'Customers', href: '/staff/customers', icon: Users, roles: null },
    { label: 'Support Chat', href: '/staff/chat', icon: MessageCircle, roles: null },
    { label: 'Messages', href: '/staff/messages', icon: MessagesSquare, roles: null },
    { label: 'Analytics', href: '/staff/analytics', icon: BarChart2, roles: ['admin'] },
    { label: 'Products', href: '/staff/products', icon: Package, roles: ['admin'] },
    { label: 'Employees', href: '/staff/staff', icon: UserCog, roles: ['admin'] },
    { label: 'Users', href: '/staff/users', icon: Shield, roles: ['admin'] },
    { label: 'My Profile', href: '/staff/profile', icon: User, roles: null }
];
```

- [ ] **Step 2: Verify type check passes**

```bash
bun check
```
Expected: no errors.

- [ ] **Step 3: Manual smoke test**

1. Log in as staff/admin
2. "Support Chat" and "Messages" links should appear in the sidebar
3. Both links should highlight when their respective pages are active

- [ ] **Step 4: Commit**

```bash
git add apps/frontend/src/lib/components/staff/StaffSidebar.svelte
git commit -m "feat: add Support Chat and Messages links to staff sidebar"
```
