import { STAFF_MESSAGES_API } from '$lib/services/constants';
import type { StaffConversation, StaffMessage } from '$lib/services/types';

export interface StaffRecipient {
    userId: string;
    username: string;
    role: string;
}

export async function listRecipients(): Promise<StaffRecipient[]> {
    const res = await fetch(`${STAFF_MESSAGES_API}/recipients`, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch recipients');
    return res.json();
}

export async function getConversations(): Promise<StaffConversation[]> {
    const res = await fetch(`${STAFF_MESSAGES_API}/conversations`, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch conversations');
    return res.json();
}

export async function startConversation(recipientId: string): Promise<StaffConversation> {
    const res = await fetch(`${STAFF_MESSAGES_API}/conversations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ recipientId })
    });
    if (!res.ok) throw new Error('Failed to start conversation');
    return res.json();
}

export async function getConvMessages(convoId: number): Promise<StaffMessage[]> {
    const res = await fetch(`${STAFF_MESSAGES_API}/conversations/${convoId}/messages`, {
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to fetch messages');
    return res.json();
}

export async function sendMessage(convoId: number, text: string): Promise<StaffMessage> {
    const res = await fetch(`${STAFF_MESSAGES_API}/conversations/${convoId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ text })
    });
    if (!res.ok) throw new Error('Failed to send message');
    return res.json();
}

export async function markConvRead(convoId: number): Promise<void> {
    const res = await fetch(`${STAFF_MESSAGES_API}/conversations/${convoId}/read`, {
        method: 'POST',
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to mark conversation as read');
}
