import { redirect } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = ({ locals, url }) => {
	if (!locals.user) return {};
	if (locals.user.role === 'admin' || locals.user.role === 'employee') {
		redirect(303, '/staff/dashboard');
	}
	const redirectTo = url.searchParams.get('redirectTo');
	redirect(303, redirectTo ?? '/profile');
};
