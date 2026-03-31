const BASE_URL = 'http://localhost:8080/api/auth';

// function to register a new user and return a response
export async function registerUser(fields) {
	const response = await fetch(`${BASE_URL}/register`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(fields)
	});

	const data = await response.json();

	return { ok: response.ok, status: response.status, data };
}

// function to log in a user and return a response
export async function loginUser(email, password) {
	const response = await fetch(`${BASE_URL}/login`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ email, password })
	});

	const data = await response.json();

	return { ok: response.ok, status: response.status, data };
}
