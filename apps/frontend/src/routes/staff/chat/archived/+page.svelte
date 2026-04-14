<script lang="ts">
    import { onMount } from 'svelte';
    import { user } from '$lib/stores/authStore';
    import { getArchivedThreads, getMessages } from '$lib/services/chat';
    import ChatMessageList from '$lib/components/chat/ChatMessageList.svelte';
    import type { ChatThread, ChatMessage } from '$lib/services/types';

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

    async function loadThreads() {
        loadingThreads = true;
        try {
            threads = await getArchivedThreads(activeCategory || undefined);
        } catch {
            // leave as-is
        } finally {
            loadingThreads = false;
        }
    }

    async function selectThread(t: ChatThread) {
        selectedThread = t;
        loadingMessages = true;
        try {
            messages = await getMessages(t.id);
        } catch {
            messages = [];
        } finally {
            loadingMessages = false;
        }
    }

    async function handleCategoryChange(cat: string) {
        activeCategory = cat;
        selectedThread = null;
        messages = [];
        await loadThreads();
    }

    onMount(loadThreads);

    function categoryLabel(cat: string): string {
        return cat.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()) || 'General';
    }

    function formatDate(iso: string): string {
        return new Date(iso).toLocaleString('en-CA', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    function formatDuration(openedAt: string, closedAt: string | null): string {
        if (!closedAt) return '';
        const ms = new Date(closedAt).getTime() - new Date(openedAt).getTime();
        const mins = Math.floor(ms / 60000);
        if (mins < 60) return `${mins}m`;
        const hrs = Math.floor(mins / 60);
        const rem = mins % 60;
        return rem > 0 ? `${hrs}h ${rem}m` : `${hrs}h`;
    }
</script>

<main class="flex flex-1 overflow-hidden">
    <!-- Left panel: thread list -->
    <aside class="flex w-72 shrink-0 flex-col border-r border-border bg-card">
        <div class="border-b border-border p-4">
            <h2 class="text-sm font-semibold text-foreground">Chat Archive</h2>
            <p class="mt-0.5 text-xs text-muted-foreground">{threads.length} closed conversation{threads.length !== 1 ? 's' : ''}</p>
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
                <div class="p-8 text-center text-xs text-muted-foreground">No archived threads</div>
            {:else}
                {#each threads as t (t.id)}
                    <button
                        onclick={() => selectThread(t)}
                        class="w-full border-b border-border p-4 text-left transition-colors hover:bg-muted {selectedThread?.id === t.id ? 'bg-muted' : ''}"
                    >
                        <p class="truncate text-sm font-medium text-foreground">
                            {t.customerDisplayName ?? t.customerUsername}
                        </p>
                        <div class="mt-1 flex items-center gap-1.5">
                            <span class="rounded-full bg-[#C4714A]/10 px-2 py-0.5 text-[10px] font-medium text-[#C4714A]">
                                {categoryLabel(t.category)}
                            </span>
                        </div>
                        <p class="mt-1 text-[10px] text-muted-foreground">
                            Closed {t.closedAt ? formatDate(t.closedAt) : formatDate(t.updatedAt)}
                        </p>
                    </button>
                {/each}
            {/if}
        </div>
    </aside>

    <!-- Right panel: receipt view -->
    <div class="flex flex-1 flex-col overflow-hidden">
        {#if !selectedThread}
            <div class="flex flex-1 items-center justify-center">
                <p class="text-sm text-muted-foreground">Select a conversation to view the receipt</p>
            </div>
        {:else}
            <!-- Receipt header -->
            <div class="shrink-0 border-b border-border bg-card px-6 py-4">
                <div class="flex items-start justify-between">
                    <div>
                        <p class="text-sm font-semibold text-foreground">
                            {selectedThread.customerDisplayName ?? selectedThread.customerUsername}
                        </p>
                        <p class="text-xs text-muted-foreground">{selectedThread.customerUsername}</p>
                    </div>
                    <span class="rounded-full bg-[#C4714A]/10 px-2.5 py-1 text-xs font-medium text-[#C4714A]">
                        {categoryLabel(selectedThread.category)}
                    </span>
                </div>
                <!-- Receipt metadata -->
                <div class="mt-3 grid grid-cols-3 gap-3 rounded-xl border border-border bg-muted/40 px-4 py-3">
                    <div>
                        <p class="text-[10px] font-medium uppercase tracking-wide text-muted-foreground">Opened</p>
                        <p class="mt-0.5 text-xs text-foreground">{formatDate(selectedThread.createdAt)}</p>
                    </div>
                    <div>
                        <p class="text-[10px] font-medium uppercase tracking-wide text-muted-foreground">Closed</p>
                        <p class="mt-0.5 text-xs text-foreground">
                            {selectedThread.closedAt ? formatDate(selectedThread.closedAt) : formatDate(selectedThread.updatedAt)}
                        </p>
                    </div>
                    <div>
                        <p class="text-[10px] font-medium uppercase tracking-wide text-muted-foreground">Duration</p>
                        <p class="mt-0.5 text-xs text-foreground">
                            {formatDuration(selectedThread.createdAt, selectedThread.closedAt ?? selectedThread.updatedAt)}
                        </p>
                    </div>
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
                    currentUserId={selectedThread.customerUserId}
                />
                <div class="shrink-0 border-t border-border bg-card px-4 py-3 text-center">
                    <p class="text-xs text-muted-foreground">{messages.length} message{messages.length !== 1 ? 's' : ''} · Conversation ended</p>
                </div>
            {/if}
        {/if}
    </div>
</main>
