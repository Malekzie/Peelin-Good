const API_BASE = '/api/v1/auth';

export async function requestPasswordReset(email) {
	const res = await fetch(`${API_BASE}/forgot-password`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ email })
	});

	return { ok: res.ok };
}

export async function validateResetToken(token) {
	const res = await fetch(`${API_BASE}/reset-password/validate?token=${encodeURIComponent(token)}`);
	return res.ok;
}

export async function resetPassword(token, newPassword) {
	const res = await fetch(`${API_BASE}/reset-password`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ token, newPassword })
	});
	if (!res.ok) {
		const data = await res.json().catch(() => ({}));
		throw new Error(data.message ?? 'Reset link is invalid or has expired.');
	}
}
