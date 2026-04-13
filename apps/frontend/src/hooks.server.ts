import { sequence } from '@sveltejs/kit/hooks';
import * as Sentry from '@sentry/sveltekit';
import type { Handle } from '@sveltejs/kit';

const VALID_ROLES = ['admin', 'employee', 'customer'] as const;
type Role = (typeof VALID_ROLES)[number];

function toRole(raw: string): Role | null {
	const normalized = raw.replace('ROLE_', '').toLowerCase();
	return (VALID_ROLES as readonly string[]).includes(normalized) ? (normalized as Role) : null;
}

export const handle: Handle = sequence(Sentry.sentryHandle(), async ({ event, resolve }) => {
	const jwt = event.cookies.get('token');

	if (jwt) {
		try {
			const payload = JSON.parse(atob(jwt.split('.')[1]));
			const now = Math.floor(Date.now() / 1000);
			if (payload.exp && payload.exp < now) {
				event.locals.user = null;
			} else {
				const roles: string[] = payload.roles ?? [];
				const rawRole = roles[0] ?? '';
				const role = toRole(rawRole);
				if (!role) {
					event.locals.user = null;
				} else {
					event.locals.user = { username: payload.sub, role };
				}
			}
		} catch {
			event.locals.user = null;
		}
	} else {
		event.locals.user = null;
	}

	return resolve(event);
});

export const handleError = Sentry.handleErrorWithSentry();
