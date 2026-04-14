import { sentrySvelteKit } from '@sentry/sveltekit';
import devtoolsJson from 'vite-plugin-devtools-json';
import tailwindcss from '@tailwindcss/vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [
		sentrySvelteKit({
			org: 'robbie-zg',
			project: 'peelin-web'
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
				target: 'ws://localhost:8080',
				ws: true,
				changeOrigin: true
			}
		}
	}
});
