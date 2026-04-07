const API_BASE = '/api/v1/auth';

export async function requestPasswordReset(email) {
	const res = await fetch(`${API_BASE}/forgot-password`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ email })
	});

	return { ok: res.ok };
}

export async function resetPassword(token, newPassword) {
	const res = await fetch(`${API_BASE}/reset-password`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ token, newPassword })
	});

	if (!res.ok) {
		const err = await res.json().catch(() => ({}));
		return { ok: false, message: err.message ?? 'Reset failed. THe link may have expired' };
	}

	return { ok: true };
}
