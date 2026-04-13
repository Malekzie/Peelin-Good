import { EMPLOYEE_STAFF_API } from '$lib/services/constants';
import type { UserRecord } from '$lib/services/types';

export async function listStaff(): Promise<UserRecord[]> {
	const res = await fetch(EMPLOYEE_STAFF_API, { credentials: 'include' });
	if (!res.ok) throw new Error('Failed to fetch staff');
	return res.json();
}
