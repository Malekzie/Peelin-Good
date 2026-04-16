import { redirect } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';
import { safeRedirectPath } from '$lib/utils/safeRedirect';

export const load: PageServerLoad = ({ locals, url }) => {
	if (!locals.user) return {};
	if (locals.user.role === 'admin' || locals.user.role === 'employee') {
		redirect(303, '/staff/dashboard');
	}
	redirect(303, safeRedirectPath(url.searchParams.get('redirectTo'), '/profile'));
};
