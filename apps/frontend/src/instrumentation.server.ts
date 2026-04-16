import * as Sentry from '@sentry/sveltekit';

const dsn = process.env.SENTRY_DSN ?? '';
const tracesSampleRate = Number(process.env.SENTRY_TRACES_SAMPLE_RATE ?? 0.1);

const SENSITIVE_KEY = /^(authorization|cookie|set-cookie|x-api-key|x-auth-token|token|access_token|refresh_token|password|secret|stripe-signature)$/i;
const SENSITIVE_QUERY = /(^|&)(token|access_token|refresh_token|code|state|id_token|password)=[^&]*/gi;

function scrubHeaders(h: Record<string, unknown> | undefined) {
	if (!h) return h;
	for (const k of Object.keys(h)) if (SENSITIVE_KEY.test(k)) h[k] = '[redacted]';
	return h;
}

Sentry.init({
	dsn,
	enabled: Boolean(dsn),
	tracesSampleRate,
	enableLogs: true,
	sendDefaultPii: false,
	beforeSend(event) {
		if (event.request) {
			event.request.cookies = undefined;
			event.request.headers = scrubHeaders(event.request.headers as Record<string, unknown>) as typeof event.request.headers;
			if (typeof event.request.query_string === 'string') {
				event.request.query_string = event.request.query_string.replace(SENSITIVE_QUERY, '$1$2=[redacted]');
			}
			if (typeof event.request.url === 'string') {
				event.request.url = event.request.url.replace(SENSITIVE_QUERY, '$1$2=[redacted]');
			}
		}
		if (event.user) {
			event.user.ip_address = undefined;
			event.user.email = undefined;
		}
		return event;
	}
});
