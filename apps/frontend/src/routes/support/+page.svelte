<script lang="ts">
	import { onMount } from 'svelte';
	import { browser } from '$app/environment';
	import { api } from '$lib/api';

	type ChatThread = {
		id: number;
		customerUserId: string;
		customerDisplayName: string | null;
		customerUsername: string | null;
		customerEmail: string | null;
		employeeUserId: string | null;
		status: string;
		createdAt: string;
		updatedAt: string;
	};

	type ChatMessage = {
		id: number;
		threadId: number;
		senderUserId: string;
		senderUsername: string | null;
		senderRole: string | null;
		text: string;
		sentAt: string;
		read: boolean;
	};

	type SocketConnectedEvent = {
		type: 'connected';
		threadId: number;
	};

	type SocketMessageEvent = {
		type: 'message';
		message: ChatMessage;
	};

	type SocketReadEvent = {
		type: 'read';
		threadId: number;
		readerUserId: string;
		messages: ChatMessage[];
	};

	type SocketErrorEvent = {
		type: 'error';
		status?: number;
		message?: string;
	};

	type SocketEvent =
		| SocketConnectedEvent
		| SocketMessageEvent
		| SocketReadEvent
		| SocketErrorEvent;

	let thread: ChatThread | null = null;
	let messages: ChatMessage[] = [];
	let draft = '';
	let loading = true;
	let sending = false;
	let error = '';
	let socketState: 'disconnected' | 'connecting' | 'connected' = 'disconnected';

	let socket: WebSocket | null = null;
	let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
	let shouldReconnect = true;
	let messagesContainer: HTMLDivElement | null = null;

	function getLocalToken(): string | null {
		if (!browser) return null;
		const token = window.localStorage.getItem('token');
		return token && token.trim().length > 0 ? token : null;
	}

	function buildWebSocketUrl(threadId: number): string {
		const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
		const host = window.location.host;
		const token = getLocalToken();

		const url = new URL(`${protocol}//${host}/ws/chat`);
		url.searchParams.set('threadId', String(threadId));

		// Needed for mobile / non-cookie fallback. Web browsers with auth cookies
		// will still send cookies on same-origin WebSocket handshakes.
		if (token) {
			url.searchParams.set('token', token);
		}

		return url.toString();
	}

	function formatTime(value: string): string {
		const date = new Date(value);
		return date.toLocaleString([], {
			year: 'numeric',
			month: 'short',
			day: 'numeric',
			hour: 'numeric',
			minute: '2-digit'
		});
	}

	function roleLabel(role: string | null): string {
		if (!role) return 'User';
		switch (role.toUpperCase()) {
			case 'ADMIN':
				return 'Admin';
			case 'EMPLOYEE':
				return 'Staff';
			case 'CUSTOMER':
				return 'Customer';
			default:
				return role;
		}
	}

	function scrollToBottom(behavior: ScrollBehavior = 'smooth') {
		queueMicrotask(() => {
			if (messagesContainer) {
				messagesContainer.scrollTo({
					top: messagesContainer.scrollHeight,
					behavior
				});
			}
		});
	}

	function replaceMessages(nextMessages: ChatMessage[]) {
		messages = [...nextMessages].sort(
			(a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
		);
	}

	function upsertMessage(nextMessage: ChatMessage) {
		const existingIndex = messages.findIndex((m) => m.id === nextMessage.id);
		if (existingIndex >= 0) {
			messages[existingIndex] = nextMessage;
			messages = [...messages];
			return;
		}

		messages = [...messages, nextMessage].sort(
			(a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
		);
	}

	function connectSocket() {
		if (!thread || !browser) return;

		if (socket) {
			socket.close();
			socket = null;
		}

		socketState = 'connecting';
		error = '';

		const ws = new WebSocket(buildWebSocketUrl(thread.id));
		socket = ws;

		ws.onopen = () => {
			socketState = 'connected';
		};

		ws.onmessage = (event) => {
			try {
				const payload = JSON.parse(event.data) as SocketEvent;

				if (payload.type === 'connected') {
					sendReadEvent();
					return;
				}

				if (payload.type === 'message') {
					upsertMessage(payload.message);
					scrollToBottom();
					sendReadEvent();
					return;
				}

				if (payload.type === 'read') {
					replaceMessages(payload.messages);
					return;
				}

				if (payload.type === 'error') {
					error = payload.message ?? 'Socket error';
				}
			} catch (e) {
				console.error('Failed to parse socket payload', e);
			}
		};

		ws.onclose = () => {
			socketState = 'disconnected';
			socket = null;

			if (shouldReconnect) {
				if (reconnectTimer) clearTimeout(reconnectTimer);
				reconnectTimer = setTimeout(() => {
					connectSocket();
				}, 1500);
			}
		};

		ws.onerror = () => {
			error = 'Live connection error';
		};
	}

	function sendReadEvent() {
		if (!socket || socket.readyState !== WebSocket.OPEN) return;
		socket.send(JSON.stringify({ type: 'read' }));
	}

	async function bootstrap() {
		loading = true;
		error = '';

		try {
			thread = await api.get<ChatThread>('/chat/threads/me/open');
			messages = await api.get<ChatMessage[]>(`/chat/threads/${thread.id}/messages`);
			scrollToBottom('auto');
			connectSocket();
		} catch (e) {
			error = e instanceof Error ? e.message : 'Failed to load support chat';
		} finally {
			loading = false;
		}
	}

	async function sendMessage() {
		const text = draft.trim();
		if (!text || sending) return;

		if (!socket || socket.readyState !== WebSocket.OPEN) {
			error = 'Live connection is not ready';
			return;
		}

		sending = true;
		error = '';

		try {
			socket.send(
				JSON.stringify({
					type: 'message',
					text
				})
			);
			draft = '';
		} catch (e) {
			error = e instanceof Error ? e.message : 'Failed to send message';
		} finally {
			sending = false;
		}
	}

	function onKeydown(event: KeyboardEvent) {
		if (event.key === 'Enter' && !event.shiftKey) {
			event.preventDefault();
			void sendMessage();
		}
	}

	function handleVisibilityChange() {
		if (document.visibilityState === 'visible') {
			sendReadEvent();
		}
	}

	onMount(() => {
		void bootstrap();

		document.addEventListener('visibilitychange', handleVisibilityChange);

		return () => {
			shouldReconnect = false;

			document.removeEventListener('visibilitychange', handleVisibilityChange);

			if (reconnectTimer) {
				clearTimeout(reconnectTimer);
			}

			if (socket) {
				socket.close();
			}
		};
	});
</script>

<svelte:head>
	<title>Support Chat | Peelin' Good</title>
</svelte:head>

<div class="mx-auto flex min-h-[calc(100vh-140px)] w-full max-w-5xl flex-col px-4 py-8">
	<div class="mb-6">
		<h1 class="text-3xl font-bold tracking-tight">Support Chat</h1>
		<p class="mt-2 text-sm text-gray-600">
			Live support for customers. Messages and read updates happen instantly.
		</p>
	</div>

	<div class="mb-4 flex items-center justify-between rounded-xl border bg-white px-4 py-3 shadow-sm">
		<div class="text-sm">
			<div class="font-medium">Connection</div>
			<div class="text-gray-600">
				{#if socketState === 'connected'}
					Live
				{:else if socketState === 'connecting'}
					Connecting...
				{:else}
					Disconnected
				{/if}
			</div>
		</div>

		{#if thread}
			<div class="text-right text-sm text-gray-600">
				<div class="font-medium">Thread #{thread.id}</div>
				<div>Status: {thread.status}</div>
			</div>
		{/if}
	</div>

	{#if loading}
		<div class="rounded-2xl border bg-white p-6 shadow-sm">
			<p class="text-sm text-gray-600">Loading support chat...</p>
		</div>
	{:else}
		<div class="flex min-h-[600px] flex-1 flex-col rounded-2xl border bg-white shadow-sm">
			<div
				bind:this={messagesContainer}
				class="flex-1 space-y-3 overflow-y-auto p-4"
			>
				{#if error}
					<div class="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
						{error}
					</div>
				{/if}

				{#if messages.length === 0}
					<div class="rounded-lg border border-dashed px-4 py-6 text-center text-sm text-gray-500">
						No messages yet. Start the conversation with support.
					</div>
				{/if}

				{#each messages as message (message.id)}
					<div class="rounded-xl border px-4 py-3">
						<div class="mb-1 flex items-center justify-between gap-3">
							<div class="min-w-0">
								<div class="truncate text-sm font-semibold">
									{message.senderUsername ?? 'Unknown User'}
								</div>
								<div class="text-xs text-gray-500">
									{roleLabel(message.senderRole)}
								</div>
							</div>
							<div class="shrink-0 text-xs text-gray-500">
								{formatTime(message.sentAt)}
							</div>
						</div>

						<div class="whitespace-pre-wrap break-words text-sm text-gray-800">
							{message.text}
						</div>

						<div class="mt-2 text-right text-xs text-gray-500">
							{message.read ? 'Read' : 'Sent'}
						</div>
					</div>
				{/each}
			</div>

			<div class="border-t p-4">
				<div class="flex flex-col gap-3">
					<textarea
						bind:value={draft}
						rows="3"
						class="w-full rounded-xl border px-4 py-3 text-sm outline-none ring-0 transition focus:border-black"
						placeholder="Type your message..."
						onkeydown={onKeydown}
					/>

					<div class="flex items-center justify-between gap-3">
						<div class="text-xs text-gray-500">
							Press Enter to send. Shift+Enter for a new line.
						</div>

						<button
							class="rounded-xl bg-black px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
							onclick={sendMessage}
							disabled={!draft.trim() || sending || socketState !== 'connected'}
						>
							{#if sending}
								Sending...
							{:else}
								Send
							{/if}
						</button>
					</div>
				</div>
			</div>
		</div>
	{/if}
</div>