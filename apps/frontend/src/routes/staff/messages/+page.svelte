<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { user } from '$lib/stores/authStore';
    import {
        getConversations,
        startConversation,
        getConvMessages,
        sendMessage,
        markConvRead,
        listRecipients,
        type StaffRecipient
    } from '$lib/services/staff-messages';
    import { subscribeWs, publishWs } from '$lib/services/ws';
    import ChatMessageList from '$lib/components/chat/ChatMessageList.svelte';
    import ChatComposer from '$lib/components/chat/ChatComposer.svelte';
    import type { StaffConversation, StaffMessage, TypingPayload } from '$lib/services/types';
    import { UserPlus } from '@lucide/svelte';

    let conversations = $state<StaffConversation[]>([]);
    let selectedConvo = $state<StaffConversation | null>(null);
    let messages = $state<StaffMessage[]>([]);
    let loadingConvos = $state(true);
    let loadingMessages = $state(false);
    let typingLabel = $state('');
    let typingTimer: ReturnType<typeof setTimeout> | undefined = undefined;
    let sendError = $state('');

    // New conversation picker state
    let showPicker = $state(false);
    let staffList = $state<StaffRecipient[]>([]);
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
        const convoId = selectedConvo.id;
        sendError = '';
        try {
            const msg = await sendMessage(convoId, text);
            if (selectedConvo?.id === convoId && !messages.find((m) => m.id === msg.id)) {
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
            staffList = await listRecipients();
        } catch {
            staffList = [];
        } finally {
            pickerLoading = false;
        }
    }

    async function handleStartConversation(staffUser: StaffRecipient) {
        showPicker = false;
        staffFilter = '';
        try {
            const convo = await startConversation(staffUser.userId);
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
                        {#each filteredStaff as su (su.userId)}
                            <button
                                onclick={() => handleStartConversation(su)}
                                class="w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-muted"
                            >
                                {su.username}
                                {#if su.role}<span class="ml-1 text-xs text-muted-foreground capitalize">({su.role.toLowerCase()})</span>{/if}
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
