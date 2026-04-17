import { env } from '$env/dynamic/public';
import { handleErrorWithSentry, replayIntegration } from '@sentry/sveltekit';
import * as Sentry from '@sentry/sveltekit';

// Conservative rates: session replay off by default; only record on error.
// Override per-env via PUBLIC_SENTRY_TRACES_SAMPLE_RATE / PUBLIC_SENTRY_REPLAY_ON_ERROR.
const tracesSampleRate = Number(env.PUBLIC_SENTRY_TRACES_SAMPLE_RATE ?? 0.1);
const replaysSessionSampleRate = Number(env.PUBLIC_SENTRY_REPLAY_SESSION_SAMPLE_RATE ?? 0);
const replaysOnErrorSampleRate = Number(env.PUBLIC_SENTRY_REPLAY_ON_ERROR_SAMPLE_RATE ?? 1.0);

// Header / query keys that must never leave the browser.
const SENSITIVE_KEYS =
	/^(authorization|cookie|set-cookie|x-api-key|x-auth-token|token|access_token|refresh_token|password|secret)$/i;
const SENSITIVE_QUERY = /^(token|access_token|refresh_token|code|state|id_token|password)$/i;

function scrub<T>(value: T): T {
	if (!value || typeof value !== 'object') return value;
	for (const key of Object.keys(value as Record<string, unknown>)) {
		if (SENSITIVE_KEYS.test(key)) {
			(value as Record<string, unknown>)[key] = '[redacted]';
		}
	}
	return value;
}

function scrubUrl(url: string | undefined): string | undefined {
	if (!url) return url;
	try {
		const u = new URL(url, 'http://_');
		let changed = false;
		u.searchParams.forEach((_, k) => {
			if (SENSITIVE_QUERY.test(k)) {
				u.searchParams.set(k, '[redacted]');
				changed = true;
			}
		});
		return changed ? (u.origin === 'http://_' ? u.pathname + u.search : u.toString()) : url;
	} catch {
		return url;
	}
}

Sentry.init({
	dsn: env.PUBLIC_SENTRY_DSN,
	enabled: Boolean(env.PUBLIC_SENTRY_DSN),

	tracesSampleRate,
	replaysSessionSampleRate,
	replaysOnErrorSampleRate,

	// Mask all user-visible content and block media in replays; never capture raw PII from forms.
	integrations: [
		replayIntegration({
			maskAllInputs: true,
			maskAllText: true,
			blockAllMedia: true
		})
	],

	// Do not attach request headers / cookies / user IP by default.
	sendDefaultPii: false,

	beforeSend(event) {
		if (event.request) {
			event.request.cookies = undefined;
			event.request.headers = scrub(event.request.headers);
			event.request.url = scrubUrl(event.request.url);
			if (event.request.query_string && typeof event.request.query_string === 'string') {
				event.request.query_string =
					scrubUrl('?' + event.request.query_string)?.replace(/^\?/, '') ??
					event.request.query_string;
			}
		}
		if (event.user) {
			event.user.ip_address = undefined;
			event.user.email = undefined;
		}
		return event;
	},

	beforeBreadcrumb(breadcrumb) {
		if (breadcrumb.data && typeof breadcrumb.data === 'object') {
			if ('url' in breadcrumb.data && typeof breadcrumb.data.url === 'string') {
				breadcrumb.data.url = scrubUrl(breadcrumb.data.url);
			}
			scrub(breadcrumb.data);
		}
		return breadcrumb;
	}
});

export const handleError = handleErrorWithSentry();
