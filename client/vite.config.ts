import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
	plugins: [tailwindcss(), sveltekit()],

	optimizeDeps: {
		include: ['svelte-sonner', 'mode-watcher', 'bits-ui']
	},
	ssr: {
		noExternal: ['svelte-sonner', 'mode-watcher', 'bits-ui']
	},
	server: {
		proxy: {
			'/api/v1': {
				target: 'http://localhost:8080',
				changeOrigin: true
			}
		}
	}
});
