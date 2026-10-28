// Arquivo temporário só para o teste local da US05 (porta 5174 -> back-end 8081). Pode apagar.
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    strictPort: true,
    proxy: {
      '/auth': 'http://localhost:8081',
      '/api': 'http://localhost:8081',
      '/pacientes': 'http://localhost:8081',
    },
  },
})
