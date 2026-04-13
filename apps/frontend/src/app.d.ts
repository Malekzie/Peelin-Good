// See https://svelte.dev/docs/kit/types#app.d.ts
// for information about these interfaces
declare global {
	namespace App {
		// interface Error {}
		interface Locals {
			user: { username: string; role: 'admin' | 'employee' | 'customer' } | null;
		}
		interface PageData {
			user?: { username: string; role: 'admin' | 'employee' | 'customer' } | null;
		}
		// interface PageState {}
		// interface Platform {}
	}
}

export {};
