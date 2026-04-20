<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { user } from '$lib/stores/authStore';
	import {
		getThreads,
		getMessages,
		postMessage,
		assignThread,
		closeThread,
		markThreadRead,
		transferThread
	} from '$lib/services/chat';
	import { listRecipients, type StaffRecipient } from '$lib/services/staff-messages';
	import { subscribeWs, publishWs } from '$lib/services/ws';
	import { ChatTopics } from '$lib/services/chatTopics';
	import ChatMessageList from '$lib/components/chat/ChatMessageList.svelte';
	import ChatComposer from '$lib/components/chat/ChatComposer.svelte';
	import type { ChatThread, ChatMessage, TypingPayload } from '$lib/services/types';
	import Button from '$lib/components/ui/button/button.svelte';
	import { Avatar, AvatarImage, AvatarFallback } from '$lib/components/ui/avatar';
	import { PanelLeftClose, PanelLeftOpen } from '@lucide/svelte';

	function initialsOf(name: string | null | undefined, fallback = '?'): string {
		const source = (name ?? '').trim();
		if (!source) return fallback;
		const parts = source.split(/\s+/).filter(Boolean);
		if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
		return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
	}

function approvedCustomerPhotoPath(thread: ChatThread): string | undefined {
	if (thread.customerPhotoApprovalPending) return undefined;
	return thread.customerProfilePhotoPath ?? undefined;
}

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
	let confirmClose = $state(false);
	let inboxOpen = $state(true);

	let showTransfer = $state(false);
	let transferStaff = $state<StaffRecipient[]>([]);
	let transferFilter = $state('');
	let transferLoading = $state(false);
	let transferSubmitting = $state(false);

	const filteredTransferStaff = $derived(
		transferStaff.filter((s) =>
			s.username.toLowerCase().includes(transferFilter.toLowerCase())
		)
	);

	const isAssignedToMe = $derived(
		!!selectedThread &&
			!!$user &&
			selectedThread.employeeUserId != null &&
			String(selectedThread.employeeUserId).toLowerCase() ===
				String($user.userId).toLowerCase()
	);

	let unsubMessages: (() => void) | null = null;
	let unsubStaffMessages: (() => void) | null = null;
	let unsubTyping: (() => void) | null = null;
	let unsubStatus: (() => void) | null = null;
	let unsubNewThreads: (() => void) | null = null;

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

	function subscribeNewThreads() {
		unsubNewThreads?.();
		unsubNewThreads = subscribeWs(ChatTopics.newThreads(), (data) => {
			const incoming = data as ChatThread;
			if (incoming.status !== 'open') return;
			if (activeCategory && incoming.category !== activeCategory) return;
			if (threads.some((t) => t.id === incoming.id)) return;
			threads = [incoming, ...threads];
		});
	}

	async function selectThread(t: ChatThread) {
		selectedThread = t;
		actionError = '';
		confirmClose = false;
		loadingMessages = true;

		unsubMessages?.();
		unsubStaffMessages?.();
		unsubTyping?.();
		unsubStatus?.();

		try {
			messages = await getMessages(t.id);
			markThreadRead(t.id).catch(() => {});
		} catch {
			messages = [];
		} finally {
			loadingMessages = false;
		}

		unsubMessages = subscribeWs(ChatTopics.messages(t.id), (data) => {
			const msg = data as ChatMessage;
			if (!messages.find((m) => m.id === msg.id)) {
				messages = [...messages, msg];
			}
		});

		unsubStaffMessages = subscribeWs(ChatTopics.staffMessages(t.id), (data) => {
			const msg = data as ChatMessage;
			if (!messages.find((m) => m.id === msg.id)) {
				messages = [...messages, msg];
			}
		});

		unsubTyping = subscribeWs(ChatTopics.typing(t.id), (data) => {
			const p = data as TypingPayload;
			if (p.userId === $user?.userId) return;
			typingLabel = 'Customer';
			clearTimeout(typingTimer);
			typingTimer = setTimeout(() => {
				typingLabel = '';
			}, 3000);
		});

		unsubStatus = subscribeWs(ChatTopics.status(t.id), (data) => {
			const updated = data as ChatThread;
			if (updated.status === 'closed' && selectedThread?.id === updated.id) {
				selectedThread = updated;
				threads = threads.filter((th) => th.id !== updated.id);
				unsubMessages?.();
				unsubTyping?.();
			}
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
		publishWs(ChatTopics.typingPublish(selectedThread.id), {
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

	async function openTransferPicker() {
		showTransfer = true;
		transferFilter = '';
		actionError = '';
		if (transferStaff.length > 0) return;
		transferLoading = true;
		try {
			transferStaff = await listRecipients();
		} catch {
			transferStaff = [];
			actionError = 'Could not load staff list.';
		} finally {
			transferLoading = false;
		}
	}

	async function handleTransfer(target: StaffRecipient) {
		if (!selectedThread || transferSubmitting) return;
		transferSubmitting = true;
		actionError = '';
		try {
			const updated = await transferThread(selectedThread.id, target.userId);
			// After transfer, the thread leaves my inbox.
			threads = threads.filter((t) => t.id !== updated.id);
			selectedThread = null;
			messages = [];
			showTransfer = false;
			unsubMessages?.();
			unsubStaffMessages?.();
			unsubTyping?.();
			unsubStatus?.();
		} catch {
			actionError = 'Failed to transfer.';
		} finally {
			transferSubmitting = false;
		}
	}

	async function handleClose() {
		if (!selectedThread) return;
		confirmClose = false;
		actionError = '';
		try {
			const updated = await closeThread(selectedThread.id);
			threads = threads.filter((t) => t.id !== updated.id);
			selectedThread = updated;
			unsubMessages?.();
			unsubTyping?.();
		} catch {
			actionError = 'Failed to close.';
		}
	}

	async function handleCategoryChange(cat: string) {
		activeCategory = cat;
		selectedThread = null;
		messages = [];
		confirmClose = false;
		await loadThreads();
	}

	onMount(() => {
		loadThreads();
		subscribeNewThreads();
	});

	onDestroy(() => {
		unsubMessages?.();
		unsubStaffMessages?.();
		unsubTyping?.();
		unsubStatus?.();
		unsubNewThreads?.();
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
	{#if inboxOpen}
		<aside class="flex w-72 shrink-0 flex-col border-r border-border bg-card">
			<div class="flex items-center justify-between border-b border-border p-4">
				<h2 class="text-sm font-semibold text-foreground">Support Inbox</h2>
				<Button
					onclick={() => (inboxOpen = false)}
					variant="ghost"
					size="icon-sm"
					aria-label="Collapse inbox"
				>
					<PanelLeftClose />
				</Button>
			</div>

			<!-- Category tabs -->
			<div class="flex flex-wrap gap-1 border-b border-border p-2">
				{#each CATEGORIES as cat (cat.value)}
					<Button
						onclick={() => handleCategoryChange(cat.value)}
						class="shrink-0 rounded-full px-3 py-1 text-xs font-medium transition-colors {activeCategory ===
						cat.value
							? 'bg-[#2C1A0E] text-[#FAF7F2]'
							: 'text-muted-foreground hover:bg-muted'}"
						variant="ghost"
					>
						{cat.label}
					</Button>
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
							class="flex w-full items-center gap-3 border-b border-border p-4 text-left transition-colors hover:bg-muted {selectedThread?.id ===
							t.id
								? 'bg-muted'
								: ''}"
						>
							<Avatar class="h-9 w-9 shrink-0">
								<AvatarImage
									src={approvedCustomerPhotoPath(t)}
									alt={t.customerDisplayName ?? t.customerUsername}
								/>
								<AvatarFallback class="bg-primary text-xs font-semibold text-primary-foreground">
									{initialsOf(t.customerDisplayName ?? t.customerUsername)}
								</AvatarFallback>
							</Avatar>
							<div class="min-w-0 flex-1">
								<p class="truncate text-sm font-medium text-foreground">
									{t.customerDisplayName ?? t.customerUsername}
								</p>
								<p class="truncate text-xs text-muted-foreground">@{t.customerUsername}</p>
								<p class="mt-0.5 text-[10px] text-muted-foreground">
									{categoryLabel(t.category)} · {formatUpdated(t.updatedAt)}
								</p>
							</div>
						</button>
					{/each}
				{/if}
			</div>
		</aside>
	{:else}
		<div class="flex shrink-0 items-start border-r border-border bg-card p-2">
			<Button
				onclick={() => (inboxOpen = true)}
				variant="ghost"
				size="icon-sm"
				aria-label="Expand inbox"
			>
				<PanelLeftOpen />
			</Button>
		</div>
	{/if}

	<!-- Right panel: thread detail -->
	<div class="flex flex-1 flex-col overflow-hidden">
		{#if !selectedThread}
			<div class="flex flex-1 items-center justify-center">
				<p class="text-sm text-muted-foreground">Select a thread to view</p>
			</div>
		{:else}
			<!-- Thread header -->
			<div
				class="flex shrink-0 items-center justify-between border-b border-border bg-card px-6 py-3"
			>
				<div class="flex items-center gap-3">
					<Avatar class="h-11 w-11">
						<AvatarImage
							src={approvedCustomerPhotoPath(selectedThread)}
							alt={selectedThread.customerDisplayName ?? selectedThread.customerUsername}
						/>
						<AvatarFallback class="bg-primary text-sm font-semibold text-primary-foreground">
							{initialsOf(
								selectedThread.customerDisplayName ?? selectedThread.customerUsername
							)}
						</AvatarFallback>
					</Avatar>
					<div>
						<p class="text-sm font-semibold text-foreground">
							{selectedThread.customerDisplayName ?? selectedThread.customerUsername}
						</p>
						<p class="text-xs text-muted-foreground">
							@{selectedThread.customerUsername} ·
							{categoryLabel(selectedThread.category)}
							{#if selectedThread.status === 'closed'}
								· <span class="text-[#C4714A]">closed</span>
							{:else if selectedThread.employeeUserId}
								· <span class="text-[#8A9E7F]">assigned</span>
							{:else}
								· <span class="text-[#C4714A]">unassigned</span>
							{/if}
						</p>
					</div>
				</div>
				<div class="flex items-center gap-2">
					{#if actionError}
						<p class="text-xs text-destructive">{actionError}</p>
					{/if}
					{#if selectedThread.status !== 'closed'}
						{#if !selectedThread.employeeUserId}
							<button
								onclick={handleAssign}
								class="rounded-lg bg-[#8A9E7F] px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-[#7a8e6f]"
							>
								Assign to me
							</button>
						{/if}
						{#if isAssignedToMe}
							<button
								onclick={openTransferPicker}
								class="rounded-lg border border-[#8A9E7F] px-3 py-1.5 text-xs font-medium text-[#8A9E7F] transition-colors hover:bg-[#8A9E7F]/10"
							>
								Transfer
							</button>
						{/if}
						{#if confirmClose}
							<span class="text-xs text-muted-foreground">Close this thread?</span>
							<button
								onclick={handleClose}
								class="rounded-lg bg-[#C4714A] px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-[#b56340]"
							>
								Yes, close
							</button>
							<button
								onclick={() => {
									confirmClose = false;
								}}
								class="rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-muted"
							>
								Cancel
							</button>
						{:else}
							<button
								onclick={() => {
									confirmClose = true;
								}}
								class="rounded-lg bg-[#C4714A] px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-[#b56340]"
							>
								Close thread
							</button>
						{/if}
					{/if}
				</div>
			</div>

			{#if showTransfer}
				<div class="border-b border-border bg-muted/30 p-3">
					<div class="mb-2 flex items-center justify-between">
						<p class="text-xs font-medium text-foreground">Transfer this conversation to:</p>
						<button
							onclick={() => {
								showTransfer = false;
							}}
							class="text-xs text-muted-foreground hover:text-foreground"
						>
							Cancel
						</button>
					</div>
					<input
						type="text"
						bind:value={transferFilter}
						placeholder="Search staff..."
						class="w-full rounded-lg border border-border bg-background px-3 py-1.5 text-sm text-foreground outline-none focus:border-[#C4714A]"
					/>
					<div class="mt-2 max-h-48 overflow-y-auto">
						{#if transferLoading}
							<p class="py-2 text-center text-xs text-muted-foreground">Loading...</p>
						{:else if filteredTransferStaff.length === 0}
							<p class="py-2 text-center text-xs text-muted-foreground">No staff found</p>
						{:else}
							{#each filteredTransferStaff as su (su.userId)}
								<button
									onclick={() => handleTransfer(su)}
									disabled={transferSubmitting}
									class="w-full rounded-lg px-3 py-2 text-left text-sm transition-colors hover:bg-muted disabled:opacity-50"
								>
									{su.username}
									{#if su.role}<span class="ml-1 text-xs text-muted-foreground capitalize"
											>({su.role.toLowerCase()})</span
										>{/if}
								</button>
							{/each}
						{/if}
					</div>
				</div>
			{/if}

			<!-- Messages -->
			{#if loadingMessages}
				<div class="flex flex-1 items-center justify-center">
					<p class="text-xs text-muted-foreground">Loading messages...</p>
				</div>
			{:else if selectedThread.status === 'closed'}
				<ChatMessageList {messages} currentUserId={String($user?.userId ?? '')} />
				<div class="border-t border-border p-4">
					<div class="rounded-xl bg-[#C4714A]/10 px-4 py-3 text-center">
						<p class="text-xs font-medium text-[#C4714A]">This conversation has been ended.</p>
					</div>
				</div>
			{:else}
				<ChatMessageList {messages} currentUserId={String($user?.userId ?? '')} {typingLabel} />
				<ChatComposer onsend={handleSend} ontyping={handleTyping} />
			{/if}
		{/if}
	</div>
</main>
