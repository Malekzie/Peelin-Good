import * as Sentry from '@sentry/sveltekit';

Sentry.init({
	dsn: 'https://272e28ecb94146b8d62d75be5f7dd594@o4508069461164032.ingest.us.sentry.io/4511153389174784',

	tracesSampleRate: 1.0,

	// Enable logs to be sent to Sentry
	enableLogs: true

	// uncomment the line below to enable Spotlight (https://spotlightjs.com)
	// spotlight: import.meta.env.DEV,
});
