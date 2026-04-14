<script lang="ts">
	import { onDestroy } from 'svelte';
	import { MessageCircle, X } from '@lucide/svelte';
	import { user } from '$lib/stores/authStore';
	import {
		getThreads,
		createThread,
		getMessages,
		postMessage,
		closeThread
	} from '$lib/services/chat';
	import { subscribeWs, publishWs } from '$lib/services/ws';
	import ChatCategoryPicker from './ChatCategoryPicker.svelte';
	import ChatMessageList from './ChatMessageList.svelte';
	import ChatComposer from './ChatComposer.svelte';
	import type { ChatThread, ChatMessage, TypingPayload } from '$lib/services/types';

	const isCustomer = $derived($user?.role === 'customer');

	let open = $state(false);
	let loading = $state(false);
	let thread = $state<ChatThread | null>(null);
	let pendingCategory = $state<string | null>(null);
	let existingThread = $state<ChatThread | null>(null);
	let messages = $state<ChatMessage[]>([]);
	let typingLabel = $state('');
	let typingTimer: ReturnType<typeof setTimeout>;
	let sendError = $state('');
	let confirmClose = $state(false);
	let closing = $state(false);
	let closeError = $state('');

	let unsubMessages: (() => void) | null = null;
	let unsubTyping: (() => void) | null = null;
	let unsubStatus: (() => void) | null = null;

	// Reset all chat state when the logged-in user changes (prevents session bleed between users)
	let _prevUserId: string | undefined;
	$effect(() => {
		const uid = $user?.userId;
		if (_prevUserId !== undefined && uid !== _prevUserId) {
			unsubMessages?.();
			unsubTyping?.();
			unsubStatus?.();
			thread = null;
			pendingCategory = null;
			existingThread = null;
			messages = [];
			open = false;
			sendError = '';
			confirmClose = false;
			closing = false;
			closeError = '';
		}
		_prevUserId = uid;
	});

	function subscribeThread(t: ChatThread) {
		unsubMessages?.();
		unsubTyping?.();
		unsubStatus?.();

		unsubMessages = subscribeWs(`/topic/chat/thread/${t.id}/messages`, (data) => {
			const msg = data as ChatMessage;
			// Deduplicate by id in case REST response and WS push overlap
			if (!messages.find((m) => m.id === msg.id)) {
				messages = [...messages, msg];
			}
		});

		unsubTyping = subscribeWs(`/topic/chat/thread/${t.id}/typing`, (data) => {
			const payload = data as TypingPayload;
			if (payload.userId === $user?.userId) return;
			typingLabel = 'Agent';
			clearTimeout(typingTimer);
			typingTimer = setTimeout(() => {
				typingLabel = '';
			}, 3000);
		});

		unsubStatus = subscribeWs(`/topic/chat/thread/${t.id}/status`, (data) => {
			const updated = data as ChatThread;
			if (updated.status === 'closed' && thread?.id === updated.id) {
				thread = updated;
				unsubMessages?.();
				unsubTyping?.();
			}
		});
	}

	async function handleOpen() {
		open = true;
		if (thread || loading) return;
		// Load any existing open thread in background to offer resume option —
		// but always show the category picker first (don't auto-enter the thread).
		loading = true;
		try {
			const threads = await getThreads();
			const threeDaysAgo = Date.now() - 3 * 24 * 60 * 60 * 1000;
			const recent = threads.find((t) => new Date(t.updatedAt).getTime() > threeDaysAgo);
			if (recent) {
				existingThread = recent;
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

	function handleCategoryPick(category: string) {
		pendingCategory = category;
		existingThread = null;
	}

	async function handleCustomerClose() {
		if (!thread || closing) return;
		closing = true;
		closeError = '';
		try {
			thread = await closeThread(thread.id);
			confirmClose = false;
			unsubMessages?.();
			unsubTyping?.();
			unsubStatus?.();
		} catch {
			closeError = 'Could not close. Try again.';
		} finally {
			closing = false;
		}
	}

	async function handleSend(text: string) {
		sendError = '';
		try {
			if (!thread) {
				if (!pendingCategory) return;
				thread = await createThread(pendingCategory);
				pendingCategory = null;
				messages = [];
				subscribeThread(thread);
			}
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
		unsubStatus?.();
		clearTimeout(typingTimer);
	});
</script>

{#if isCustomer}
	<div class="fixed right-6 bottom-6 z-50">
		{#if open}
			<div
				class="flex h-[480px] w-80 flex-col overflow-hidden rounded-2xl border border-[#2C1A0E]/10 bg-[#FAF7F2] shadow-2xl dark:border-[#FAF7F2]/10 dark:bg-[#2C1A0E]"
			>
				<!-- Header -->
				<div class="flex shrink-0 items-center justify-between bg-[#2C1A0E] px-4 py-3">
					<p class="text-sm font-semibold text-[#FAF7F2]">Support Chat</p>
					<button
						onclick={() => {
							open = false;
						}}
						class="rounded p-0.5 text-[#FAF7F2]/70 hover:text-[#FAF7F2]"
						aria-label="Close chat"
					>
						<X class="h-4 w-4" />
					</button>
				</div>

				<!-- Body -->
				{#if loading}
					<div class="flex flex-1 items-center justify-center">
						<p class="text-xs text-[#2C1A0E]/40 dark:text-[#FAF7F2]/40">Loading...</p>
					</div>
				{:else if thread?.status === 'closed'}
					<div class="flex flex-1 flex-col">
						<ChatMessageList {messages} currentUserId={String($user?.userId ?? '')} />
						<div class="border-t border-[#2C1A0E]/10 dark:border-[#FAF7F2]/10">
							<div class="m-3 rounded-xl bg-[#C4714A]/10 px-4 py-3 text-center">
								<p class="text-xs font-medium text-[#C4714A]">This conversation has been ended.</p>
							</div>
							<div class="pb-3 text-center">
								<button
									onclick={() => {
										unsubStatus?.();
										thread = null;
										pendingCategory = null;
										messages = [];
									}}
									class="text-xs font-medium text-[#C4714A] hover:underline"
								>
									Start a new conversation
								</button>
							</div>
						</div>
					</div>
				{:else if pendingCategory}
					<div
						class="flex shrink-0 items-center justify-between border-b border-[#2C1A0E]/10 px-4 py-2 dark:border-[#FAF7F2]/10"
					>
						<p class="text-xs text-[#2C1A0E]/60 dark:text-[#FAF7F2]/60">
							Topic: <span class="font-medium text-[#2C1A0E] capitalize dark:text-[#FAF7F2]"
								>{pendingCategory.replace('_', ' ')}</span
							>
						</p>
						<button
							onclick={() => {
								pendingCategory = null;
								sendError = '';
							}}
							class="text-xs text-[#C4714A] hover:underline"
						>
							Change
						</button>
					</div>
					<div class="flex flex-1 items-center justify-center">
						<p class="px-4 text-center text-xs text-[#2C1A0E]/40 dark:text-[#FAF7F2]/40">
							Type your first message to start the conversation.
						</p>
					</div>
					{#if sendError}
						<p class="px-3 pb-1 text-center text-xs text-red-500">{sendError}</p>
					{/if}
					<ChatComposer onsend={handleSend} ontyping={handleTyping} />
				{:else if !thread}
					<ChatCategoryPicker
						onpick={handleCategoryPick}
						{existingThread}
						onresume={handleResume}
					/>
				{:else}
					<ChatMessageList {messages} currentUserId={String($user?.userId ?? '')} {typingLabel} />
					{#if sendError}
						<p class="px-3 pb-1 text-center text-xs text-red-500">{sendError}</p>
					{/if}
					<ChatComposer onsend={handleSend} ontyping={handleTyping} />
					<div class="border-t border-[#2C1A0E]/10 px-3 py-2 text-center dark:border-[#FAF7F2]/10">
						{#if confirmClose}
							{#if closeError}
								<p class="mb-1 text-xs text-red-500">{closeError}</p>
							{/if}
							<p class="text-xs text-[#2C1A0E]/70 dark:text-[#FAF7F2]/70">
								Close this conversation?
							</p>
							<div class="mt-1.5 flex justify-center gap-4">
								<button
									onclick={handleCustomerClose}
									disabled={closing}
									class="text-xs font-medium text-[#C4714A] hover:underline disabled:opacity-50"
								>
									{closing ? 'Closing...' : 'Yes, close'}
								</button>
								<button
									onclick={() => {
										confirmClose = false;
										closeError = '';
									}}
									disabled={closing}
									class="text-xs text-[#2C1A0E]/50 hover:text-[#2C1A0E] disabled:opacity-50 dark:text-[#FAF7F2]/50 dark:hover:text-[#FAF7F2]"
								>
									Cancel
								</button>
							</div>
						{:else}
							<button
								onclick={() => {
									confirmClose = true;
								}}
								class="text-xs text-[#2C1A0E]/40 transition-colors hover:text-[#2C1A0E] dark:text-[#FAF7F2]/40 dark:hover:text-[#FAF7F2]"
							>
								Close conversation
							</button>
						{/if}
					</div>
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
