<script>
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { setAuth } from '$lib/stores/authStore.js';

	function getDefaultPostAuthRoute(role) {
		const normalizedRole = (role ?? '').toLowerCase();
		return normalizedRole === 'admin' ||
			normalizedRole === 'employee' ||
			normalizedRole.endsWith('_admin') ||
			normalizedRole.endsWith('_employee')
			? '/staff/dashboard'
			: '/profile';
	}

	onMount(() => {
		const params = new URLSearchParams(window.location.search);
		const username = params.get('username');
		const role = params.get('role');
		const userId = params.get('userId');

		if (username && role && userId) {
			setAuth({ username, role, userId });
			goto(resolve(getDefaultPostAuthRoute(role)));
		} else {
			goto(resolve('/login?error=oauth_failed'));
		}
	});
</script>

<div class="flex min-h-screen items-center justify-center">
	<div class="text-center">
		<div
			class="mx-auto mb-4 h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent"
		></div>
		<p class="text-on-surface-variant text-sm">Signing you in...</p>
	</div>
</div>
