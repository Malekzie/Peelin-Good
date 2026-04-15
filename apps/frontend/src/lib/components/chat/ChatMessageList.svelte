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

	let listEl: HTMLDivElement | undefined;

	$effect(() => {
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
		if (!currentUserId) return false;
		return String(msg.senderUserId).toLowerCase() === String(currentUserId).toLowerCase();
	}
</script>

<div bind:this={listEl} class="flex flex-1 flex-col gap-2 overflow-y-auto p-3">
	{#each messages as msg (msg.id)}
		{@const mine = isMine(msg)}
		<div class={['flex', mine ? 'justify-end' : 'justify-start']}>
			<div class="max-w-[75%]">
				<div
					class={[
						'rounded-2xl px-3 py-2 text-sm',
						mine && 'rounded-tr-sm bg-[#C4714A] text-white',
						!mine &&
							'rounded-tl-sm bg-card text-[#2C1A0E] shadow-sm dark:bg-[#FAF7F2]/10 dark:text-[#FAF7F2]'
					]}
				>
					{msg.text}
				</div>
				<p
					class={[
						'mt-0.5 px-1 text-[10px] text-[#2C1A0E]/40 dark:text-[#FAF7F2]/40',
						mine ? 'text-right' : 'text-left'
					]}
				>
					{formatTime(msg.sentAt)}
				</p>
			</div>
		</div>
	{/each}

	{#if typingLabel}
		<div class="flex justify-start">
			<div
				class="rounded-2xl rounded-tl-sm bg-card px-3 py-2 text-xs text-[#2C1A0E]/50 shadow-sm dark:bg-[#FAF7F2]/10 dark:text-[#FAF7F2]/50"
			>
				{typingLabel} is typing...
			</div>
		</div>
	{/if}

	{#if messages.length === 0 && !typingLabel}
		<div class="flex flex-1 items-center justify-center">
			<p class="text-xs text-[#2C1A0E]/40 dark:text-[#FAF7F2]/40">No messages yet</p>
		</div>
	{/if}
</div>
