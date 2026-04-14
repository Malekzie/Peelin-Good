import { Client, type StompSubscription } from '@stomp/stompjs';
import { browser } from '$app/environment';

let _client: Client | null = null;

interface SubEntry {
    cb: (data: unknown) => void;
    sub: StompSubscription | null;
}

// topic → subscription entry (persisted across reconnects)
const subscriptions = new Map<string, SubEntry>();

function clearActiveSubs() {
    for (const entry of subscriptions.values()) {
        entry.sub = null;
    }
}

function buildClient(): Client {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const brokerURL = `${protocol}//${window.location.host}/ws/websocket`;

    const c = new Client({
        brokerURL,
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000
    });

    c.onConnect = () => {
        for (const [topic, entry] of subscriptions) {
            if (!entry.sub) {
                entry.sub = c.subscribe(topic, (frame) => {
                    try {
                        entry.cb(JSON.parse(frame.body));
                    } catch {
                        entry.cb(frame.body);
                    }
                });
            }
        }
    };

    // Use onWebSocketClose instead of onDisconnect — onDisconnect only fires on graceful
    // STOMP DISCONNECT receipt, which is not sent on network drops or heartbeat timeouts.
    c.onWebSocketClose = () => {
        clearActiveSubs();
    };

    return c;
}

export function connectWs(): void {
    if (!browser) return;
    if (!_client) _client = buildClient();
    if (!_client.active) _client.activate();
}

export async function disconnectWs(): Promise<void> {
    if (!browser || !_client) return;
    // Capture ref before nulling so a concurrent connectWs() can't see the old client
    const c = _client;
    _client = null;
    subscriptions.clear();
    await c.deactivate();
}

/**
 * Subscribe to a STOMP topic. Returns an unsubscribe function.
 * Safe to call before the client connects — subscription is replayed on connect.
 * Calling with the same topic replaces the previous callback and unsubscribes the old one.
 */
export function subscribeWs(topic: string, callback: (data: unknown) => void): () => void {
    if (!browser) return () => {};

    // Unsubscribe any existing subscription on this topic before replacing
    const previous = subscriptions.get(topic);
    if (previous?.sub) {
        previous.sub.unsubscribe();
    }

    const entry: SubEntry = { cb: callback, sub: null };
    subscriptions.set(topic, entry);

    if (_client?.connected) {
        entry.sub = _client.subscribe(topic, (frame) => {
            try {
                entry.cb(JSON.parse(frame.body));
            } catch {
                entry.cb(frame.body);
            }
        });
    }

    return () => {
        entry.sub?.unsubscribe();
        subscriptions.delete(topic);
    };
}

export function publishWs(destination: string, body: unknown): void {
    if (!browser || !_client?.connected) return;
    _client.publish({ destination, body: JSON.stringify(body) });
}
