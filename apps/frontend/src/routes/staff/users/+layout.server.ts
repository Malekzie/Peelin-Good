import { redirect } from '@sveltejs/kit';
import type { LayoutServerLoad } from './$types';

export const load: LayoutServerLoad = ({ locals }) => {
	if (!locals.user || locals.user.role !== 'admin') {
		redirect(303, '/staff/dashboard');
	}
	return { user: locals.user };
};
