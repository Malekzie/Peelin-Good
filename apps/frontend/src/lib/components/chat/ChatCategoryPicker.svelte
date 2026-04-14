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
