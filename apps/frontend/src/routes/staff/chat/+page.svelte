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
    let typingTimer: ReturnType<typeof setTimeout> | undefined = undefined;
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
        const threadId = selectedThread.id;
        actionError = '';
        try {
            const msg = await postMessage(threadId, text);
            if (selectedThread?.id === threadId && !messages.find((m) => m.id === msg.id)) {
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
            unsubMessages?.();
            unsubTyping?.();
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
                <div class="p-4 text-center text-xs text-muted-foreground">Loading...</div>
            {:else if threads.length === 0}
                <div class="p-8 text-center text-xs text-muted-foreground">No open threads</div>
            {:else}
                {#each threads as t (t.id)}
                    <button
                        onclick={() => selectThread(t)}
                        class="w-full border-b border-border p-4 text-left transition-colors hover:bg-muted {selectedThread?.id === t.id
                            ? 'bg-muted'
                            : ''}"
                    >
                        <p class="truncate text-sm font-medium text-foreground">
                            {t.customerDisplayName ?? t.customerUsername}
                        </p>
                        <p class="mt-0.5 text-xs text-muted-foreground">{categoryLabel(t.category)}</p>
                        <p class="mt-0.5 text-[10px] text-muted-foreground">{formatUpdated(t.updatedAt)}</p>
                    </button>
                {/each}
            {/if}
        </div>
    </aside>

    <!-- Right panel: thread detail -->
    <div class="flex flex-1 flex-col overflow-hidden">
        {#if !selectedThread}
            <div class="flex flex-1 items-center justify-center">
                <p class="text-sm text-muted-foreground">Select a thread to view</p>
            </div>
        {:else}
            <!-- Thread header -->
            <div class="flex shrink-0 items-center justify-between border-b border-border bg-card px-6 py-3">
                <div>
                    <p class="text-sm font-semibold text-foreground">
                        {selectedThread.customerDisplayName ?? selectedThread.customerUsername}
                    </p>
                    <p class="text-xs text-muted-foreground">{categoryLabel(selectedThread.category)}</p>
                </div>
                <div class="flex items-center gap-2">
                    {#if actionError}
                        <p class="text-xs text-destructive">{actionError}</p>
                    {/if}
                    {#if !selectedThread.employeeUserId}
                        <button
                            onclick={handleAssign}
                            class="rounded-lg bg-[#8A9E7F] px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-[#7a8e6f]"
                        >
                            Assign to me
                        </button>
                    {/if}
                    <button
                        onclick={handleClose}
                        class="rounded-lg bg-[#C4714A] px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-[#b56340]"
                    >
                        Close thread
                    </button>
                </div>
            </div>

            <!-- Messages -->
            {#if loadingMessages}
                <div class="flex flex-1 items-center justify-center">
                    <p class="text-xs text-muted-foreground">Loading messages...</p>
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
