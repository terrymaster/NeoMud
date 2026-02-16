import express from 'express'
import { projectsRouter } from './routes/projects.js'
import { zonesRouter } from './routes/zones.js'
import { exportRouter } from './routes/export.js'

const app = express()
app.use(express.json())

app.use('/api/projects', projectsRouter)
app.use('/api', zonesRouter)
app.use('/api/export', exportRouter)

const port = parseInt(process.env.MAKER_PORT || '3001', 10)

const server = app.listen(port, () => {
  console.log(`NeoMUDMaker API server running on http://localhost:${port}`)
})

server.on('error', (err: NodeJS.ErrnoException) => {
  if (err.code === 'EADDRINUSE') {
    console.error(`Port ${port} is already in use. Set MAKER_PORT env var to use a different port.`)
    process.exit(1)
  }
  throw err
})
