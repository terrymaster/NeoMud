import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    testTimeout: 30000,
    hookTimeout: 30000,
    globalSetup: ['./server/__tests__/globalSetup.ts'],
    environmentMatchGlobs: [
      ['src/**/*.test.tsx', 'jsdom'],
      ['src/**/*.test.ts', 'jsdom'],
    ],
    setupFiles: ['./src/__tests__/setup.ts'],
    css: false,
    exclude: ['e2e/**', 'node_modules/**'],
  },
})
