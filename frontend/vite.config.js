import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite config: dev server on 5173, production build output to dist/
// which the Nginx stage of the Dockerfile serves as static files.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true
  },
  preview: {
    port: 5173,
    host: true
  }
})
