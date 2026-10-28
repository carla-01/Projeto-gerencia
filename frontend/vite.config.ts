/// <reference types="vitest" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Testes com Vitest (npm test), rodados também no CI.
  test: {
    environment: 'jsdom',
    setupFiles: './src/setupTests.ts',
  },
})
