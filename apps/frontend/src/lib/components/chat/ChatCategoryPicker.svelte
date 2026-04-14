<script lang="ts">
    import type { ChatThread } from '$lib/services/types';

    let {
        onpick,
        existingThread = null,
        onresume = null
    }: {
        onpick: (category: string) => void;
        existingThread?: ChatThread | null;
        onresume?: (() => void) | null;
    } = $props();

    const categories = [
        { value: 'general', label: 'General Support' },
        { value: 'order_issue', label: 'Order Issue' },
        { value: 'account_help', label: 'Account Help' },
        { value: 'feedback', label: 'Feedback' }
    ];
</script>

<div class="flex flex-1 flex-col gap-3 overflow-y-auto p-4">
    {#if existingThread && onresume}
        <div class="rounded-xl border border-[#C4714A]/30 bg-[#C4714A]/5 p-3">
            <p class="text-xs font-medium text-[#2C1A0E]/60">Previous conversation</p>
            <p class="mt-0.5 text-sm font-medium capitalize text-[#2C1A0E]">
                {existingThread.category?.replace('_', ' ') ?? 'General Support'}
            </p>
            <button
                onclick={onresume}
                class="mt-2 text-xs font-semibold text-[#C4714A] hover:underline"
            >
                Resume conversation
            </button>
        </div>
        <p class="text-xs text-[#2C1A0E]/50">Or start a new one:</p>
    {:else}
        <p class="text-sm font-medium text-[#2C1A0E]">How can we help you today?</p>
    {/if}
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
