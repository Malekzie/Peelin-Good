import { redirect } from '@sveltejs/kit';
import type { LayoutServerLoad } from './$types';

export const load: LayoutServerLoad = ({ locals, url }) => {
	if (!locals.user) {
		redirect(303, `/login?redirectTo=${encodeURIComponent(url.pathname)}`);
	}
	if (locals.user.role !== 'employee' && locals.user.role !== 'admin') {
		redirect(303, '/');
	}
	return { user: locals.user };
};
