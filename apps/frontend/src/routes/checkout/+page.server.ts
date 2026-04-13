import { redirect } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

// Page-level guard (not layout) so /checkout/guest remains accessible without auth.
export const load: PageServerLoad = ({ locals, url }) => {
	if (!locals.user) {
		redirect(303, `/login?redirectTo=${encodeURIComponent(url.pathname)}`);
	}
	return { user: locals.user };
};
