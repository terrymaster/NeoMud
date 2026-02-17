import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'

function makerApiPlugin(): Plugin {
  return {
    name: 'maker-api',
    async configureServer(server) {
      const { app, autoImportDefaultWorld } = await import('./server/app.js')
      await autoImportDefaultWorld()
      // Mount the Express app as Vite middleware
      server.middlewares.use(app)
      console.log('[vite] Maker API mounted as middleware')
    },
  }
}

export default defineConfig({
  plugins: [react(), makerApiPlugin()],
  server: {
    strictPort: true,
  },
})
