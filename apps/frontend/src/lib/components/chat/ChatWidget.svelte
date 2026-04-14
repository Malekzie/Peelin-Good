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
    let existingThread = $state<ChatThread | null>(null);
    let messages = $state<ChatMessage[]>([]);
    let typingLabel = $state('');
    let typingTimer: ReturnType<typeof setTimeout>;
    let sendError = $state('');

    let unsubMessages: (() => void) | null = null;
    let unsubTyping: (() => void) | null = null;

    // Reset all chat state when the logged-in user changes (prevents session bleed between users)
    let _prevUserId: string | undefined;
    $effect(() => {
        const uid = $user?.userId;
        if (_prevUserId !== undefined && uid !== _prevUserId) {
            unsubMessages?.();
            unsubTyping?.();
            thread = null;
            existingThread = null;
            messages = [];
            open = false;
            sendError = '';
        }
        _prevUserId = uid;
    });

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
        if (thread || loading) return;
        // Load any existing open thread in background to offer resume option —
        // but always show the category picker first (don't auto-enter the thread).
        loading = true;
        try {
            const threads = await getThreads();
            if (threads.length > 0) {
                existingThread = threads[0];
            }
        } catch {
            // no existing thread — show picker as normal
        } finally {
            loading = false;
        }
    }

    async function handleResume() {
        if (!existingThread) return;
        loading = true;
        try {
            thread = existingThread;
            messages = await getMessages(thread.id);
            subscribeThread(thread);
        } catch {
            // stay on picker
        } finally {
            loading = false;
        }
    }

    async function handleCategoryPick(category: string) {
        loading = true;
        try {
            thread = await createThread(category);
            existingThread = null;
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
                                onclick={() => { unsubMessages?.(); unsubTyping?.(); thread = null; messages = []; }}
                                class="mt-2 text-xs font-medium text-[#C4714A] hover:underline"
                            >
                                Start a new conversation
                            </button>
                        </div>
                    </div>
                {:else if !thread}
                    <ChatCategoryPicker
                        onpick={handleCategoryPick}
                        {existingThread}
                        onresume={handleResume}
                    />
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
