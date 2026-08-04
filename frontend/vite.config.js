import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Port 5500 is important - the backend's SecurityConfig.java trusts
// http://localhost:5500 specifically for CORS and post-login redirects.
// If you ever change this port, update FRONTEND_URL in SecurityConfig.java
// to match.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5500,
    strictPort: true
  }
});
