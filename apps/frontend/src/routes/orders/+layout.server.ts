import type { LayoutServerLoad } from './$types';

// No auth guard here — /orders/[number]/confirmation must be accessible to guests.
// Individual pages guard themselves where auth is required.
export const load: LayoutServerLoad = ({ locals }) => {
	return { user: locals.user };
};
