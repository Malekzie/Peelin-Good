const API = '/api/v1';

export async function listStaff() {
	const res = await fetch(`${API}/employee/staff`, { credentials: 'include' });
	if (!res.ok) throw new Error('Failed to fetch staff');
	return res.json();
}

export async function deleteEmployee(id) {
	const res = await fetch(`${API}/admin/employees/${id}`, {
		method: 'DELETE',
		credentials: 'include'
	});
	if (!res.ok) throw new Error('Failed to delete employee');
}
