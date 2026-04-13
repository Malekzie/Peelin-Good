import { AUTH_API, PASSWORD_RESET_API } from '$lib/services/constants';

export async function requestPasswordReset(email: string): Promise<{ ok: boolean }> {
	const res = await fetch(`${AUTH_API}/forgot-password`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ email })
	});

	return { ok: res.ok };
}

export async function validateResetToken(token: string): Promise<boolean> {
	const res = await fetch(`${PASSWORD_RESET_API}/validate?token=${encodeURIComponent(token)}`);
	return res.ok;
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
	const res = await fetch(PASSWORD_RESET_API, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ token, newPassword })
	});
	if (!res.ok) {
		const data = (await res.json().catch(() => ({}))) as { message?: string };
		throw new Error(data.message ?? 'Reset link is invalid or has expired.');
	}
}
