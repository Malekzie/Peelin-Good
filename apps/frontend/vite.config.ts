import { sentrySvelteKit } from '@sentry/sveltekit';
import devtoolsJson from 'vite-plugin-devtools-json';
import tailwindcss from '@tailwindcss/vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [
		sentrySvelteKit({
			org: 'robbie-zg',
			project: 'peelin-web',
			// Tag uploaded source-maps with the same release the runtime reports so Sentry
			// can match minified frames back to source. SENTRY_RELEASE is set in CI to the
			// commit SHA; sentryVitePlugin reads SENTRY_AUTH_TOKEN from the env.
			sourceMapsUploadOptions: {
				release: { name: process.env.SENTRY_RELEASE }
			}
		}),
		tailwindcss(),
		sveltekit(),
		devtoolsJson()
	],
	server: {
		proxy: {
			'/api': {
				target: 'http://localhost:8080',
				changeOrigin: true
			},
			'/oauth2': {
				target: 'http://localhost:8080',
				changeOrigin: true
			},
			'/login/oauth2': {
				target: 'http://localhost:8080',
				changeOrigin: true
			},
			'/ws': {
				target: 'http://localhost:8080',
				changeOrigin: true,
				ws: true
			}
		}
	}
});
