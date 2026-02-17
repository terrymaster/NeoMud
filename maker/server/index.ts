import { app, autoImportDefaultWorld } from './app.js'
import { disconnectDb } from './db.js'

const port = parseInt(process.env.MAKER_PORT || '3001', 10)

await autoImportDefaultWorld()

const server = app.listen(port, () => {
  console.log(`NeoMUDMaker API server running on http://localhost:${port}`)
})

// Graceful shutdown
let shuttingDown = false
function shutdown() {
  if (shuttingDown) return
  shuttingDown = true
  console.log('[shutdown] Closing maker server...')
  server.close(async () => {
    await disconnectDb()
    process.exit(0)
  })
  setTimeout(() => process.exit(0), 3000)
}

process.on('SIGINT', shutdown)
process.on('SIGTERM', shutdown)
process.on('SIGBREAK', shutdown)
