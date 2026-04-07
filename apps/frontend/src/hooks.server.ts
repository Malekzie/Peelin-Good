import { sequence } from '@sveltejs/kit/hooks';
import * as Sentry from '@sentry/sveltekit';
import { redirect } from '@sveltejs/kit';
import type { Handle } from '@sveltejs/kit';

export const handle: Handle = sequence(Sentry.sentryHandle(), async ({ event, resolve }) => {
	const jwt = event.cookies.get('jwt');

	if (jwt) {
		try {
			const payload = JSON.parse(atob(jwt.split('.')[1]));
			const roles: string[] = payload.roles ?? [];
			const rawRole = roles[0] ?? '';
			const role = rawRole.replace('ROLE_', '').toLowerCase() as 'admin' | 'employee' | 'customer';
			event.locals.user = { username: payload.sub, role };
		} catch {
			event.locals.user = null;
		}
	} else {
		event.locals.user = null;
	}

	const { pathname } = event.url;
	const user = event.locals.user;

	if (pathname.startsWith('/account')) {
		if (!user) throw redirect(303, '/login');
	} else if (pathname.startsWith('/employee')) {
		if (!user || (user.role !== 'employee' && user.role !== 'admin')) {
			throw redirect(303, '/?error=forbidden');
		}
	} else if (pathname.startsWith('/admin')) {
		if (!user || user.role !== 'admin') {
			throw redirect(303, '/?error=forbidden');
		}
	}

	return resolve(event);
});
export const handleError = Sentry.handleErrorWithSentry();
