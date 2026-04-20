import { sequence } from '@sveltejs/kit/hooks';
import * as Sentry from '@sentry/sveltekit';
import type { Handle } from '@sveltejs/kit';
import { renderMissingEnvPage, validateEnv } from '$lib/server/envValidator';

const VALID_ROLES = ['admin', 'employee', 'customer'] as const;
type Role = (typeof VALID_ROLES)[number];

function toRole(raw: string): Role | null {
	const normalized = raw.replace('ROLE_', '').toLowerCase();
	return (VALID_ROLES as readonly string[]).includes(normalized) ? (normalized as Role) : null;
}

// Validate once at module load so the banner prints on first boot, then cache the result.
const envValidation = validateEnv();

const envGuard: Handle = async ({ event, resolve }) => {
	if (envValidation.isProd && envValidation.missing.length > 0) {
		return new Response(renderMissingEnvPage(envValidation), {
			status: 503,
			headers: {
				'content-type': 'text/html; charset=utf-8',
				'cache-control': 'no-store'
			}
		});
	}
	return resolve(event);
};

export const handle: Handle = sequence(
	envGuard,
	Sentry.sentryHandle(),
	async ({ event, resolve }) => {
		const jwt = event.cookies.get('token');

		if (jwt) {
			try {
				const b64 = jwt.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
				const payload = JSON.parse(Buffer.from(b64, 'base64').toString('utf8'));
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
	}
);

export const handleError = Sentry.handleErrorWithSentry();
