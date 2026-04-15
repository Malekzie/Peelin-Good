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

<div class="flex items-end gap-2 border-t border-[#2C1A0E]/10 bg-[#FAF7F2] p-3 dark:border-[#FAF7F2]/10 dark:bg-[#2C1A0E]">
	<textarea
		bind:value={text}
		oninput={() => ontyping?.()}
		onkeydown={handleKeydown}
		{placeholder}
		{disabled}
		rows="1"
		class="flex-1 resize-none rounded-xl border border-[#2C1A0E]/15 bg-card px-3 py-2 text-sm text-[#2C1A0E] placeholder-[#2C1A0E]/30 outline-none focus:border-[#C4714A] disabled:opacity-50 dark:border-[#FAF7F2]/15 dark:bg-[#FAF7F2]/10 dark:text-[#FAF7F2] dark:placeholder-[#FAF7F2]/30"
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
