/// <reference types="vitest" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Em desenvolvimento, as chamadas da API vão para o back-end Spring na porta 8080.
    // Atenção: o caminho da API não pode começar igual a uma rota de tela (ex.: /cadastro).
    proxy: {
      '/auth': 'http://localhost:8080',
      '/api': 'http://localhost:8080',
      '/pacientes': 'http://localhost:8080',
    },
  },
  // Testes com Vitest (npm test), rodados também no CI.
  test: {
    environment: 'jsdom',
    setupFiles: './src/setupTests.ts',
  },
})