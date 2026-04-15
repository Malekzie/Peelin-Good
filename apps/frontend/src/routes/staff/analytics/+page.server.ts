import type { PageServerLoad } from './$types';

export const load: PageServerLoad = () => {
	const today = new Date().toISOString().split('T')[0];
	const yearAgo = new Date(Date.now() - 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

	return {
		startDate: yearAgo,
		endDate: today
	};
};
