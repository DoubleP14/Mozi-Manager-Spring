import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [
		sveltekit({
			compilerOptions: {
				// Force runes mode for the project, except for libraries. Can be removed in svelte 6.
				runes: ({ filename }) =>
					filename.split(/[/\\]/).includes('node_modules') ? undefined : true
			},

			// Egyoldalas alkalmazas (SPA): a build statikus fajlokat ad, minden utvonal az index.html-re esik vissza
			adapter: adapter({ fallback: 'index.html' })
		})
	],
	server: {
		port: 5173,
		// A /api kereseket a Spring Boot backend kapja. Igy a bongeszo szamara egy helyrol jon minden,
		// a session es a CSRF cookie gond nelkul mukodik (nem kell CORS).
		proxy: {
			'/api': 'http://localhost:8090'
		}
	}
});
