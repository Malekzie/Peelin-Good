import { CHAT_API } from '$lib/services/constants';
import type { ChatMessage, ChatThread } from '$lib/services/types';

/** Customer: returns their open thread(s) (0 or 1). Staff: returns all open threads. */
export async function getThreads(category?: string): Promise<ChatThread[]> {
    const url = category ? `${CHAT_API}/threads?category=${encodeURIComponent(category)}` : `${CHAT_API}/threads`;
    const res = await fetch(url, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch threads');
    return res.json();
}

/** Customer creates a new support thread with the selected category. */
export async function createThread(category: string): Promise<ChatThread> {
    const res = await fetch(`${CHAT_API}/threads`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ category })
    });
    if (!res.ok) throw new Error('Failed to create thread');
    return res.json();
}

export async function getMessages(threadId: number): Promise<ChatMessage[]> {
    const res = await fetch(`${CHAT_API}/threads/${threadId}/messages`, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch messages');
    return res.json();
}

export async function postMessage(threadId: number, text: string): Promise<ChatMessage> {
    const res = await fetch(`${CHAT_API}/threads/${threadId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ text })
    });
    if (!res.ok) throw new Error('Failed to post message');
    return res.json();
}

export async function assignThread(threadId: number): Promise<ChatThread> {
    const res = await fetch(`${CHAT_API}/threads/${threadId}/assign`, {
        method: 'POST',
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to assign thread');
    return res.json();
}

export async function closeThread(threadId: number): Promise<ChatThread> {
    const res = await fetch(`${CHAT_API}/threads/${threadId}/close`, {
        method: 'POST',
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to close thread');
    return res.json();
}

export async function getArchivedThreads(category?: string): Promise<ChatThread[]> {
    const url = category
        ? `${CHAT_API}/threads/archived?category=${encodeURIComponent(category)}`
        : `${CHAT_API}/threads/archived`;
    const res = await fetch(url, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch archived threads');
    return res.json();
}

export async function markThreadRead(threadId: number): Promise<void> {
    await fetch(`${CHAT_API}/threads/${threadId}/read`, {
        method: 'POST',
        credentials: 'include'
    });
}
