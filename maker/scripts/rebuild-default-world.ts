/**
 * Headless script to rebuild maker/default_world.nmd from its own contents.
 *
 * Usage:  cd maker && npm run rebuild-world
 *
 * Flow:
 *   1. Delete temp project "_rebuild_default_world" if leftover from a previous run
 *   2. If default_world.nmd exists -> import it (preserves all world data + assets)
 *      Otherwise -> createProject() (seeds 270 PC sprites automatically)
 *   3. Export via buildNmdBundle() -> write to maker/default_world.nmd
 *   4. Clean up temp project and disconnect
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { createProject, db, deleteProject, disconnectDb, getActiveProject } from '../server/db.js'
import { importNmd } from '../server/import.js'
import { buildNmdBundle } from '../server/routes/export.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const TEMP_PROJECT = '_rebuild_default_world'
const NMD_PATH = path.resolve(__dirname, '..', 'default_world.nmd')

async function main() {
  try {
    // 1. Clean up any leftover temp project
    await deleteProject(TEMP_PROJECT)

    // 2. Import existing bundle or create fresh project
    if (fs.existsSync(NMD_PATH)) {
      console.log(`Importing existing bundle: ${NMD_PATH}`)
      await importNmd(NMD_PATH, TEMP_PROJECT)
    } else {
      console.log('No existing bundle found — creating fresh project with seeded data.')
      await createProject(TEMP_PROJECT)
    }

    // 3. Export
    const prisma = db()
    const projectName = getActiveProject()!
    const buffer = await buildNmdBundle(prisma, projectName)

    fs.writeFileSync(NMD_PATH, buffer)
    const sizeMB = (buffer.length / (1024 * 1024)).toFixed(1)
    console.log(`Wrote ${sizeMB} MB to ${NMD_PATH}`)

    // 4. Clean up
    await deleteProject(TEMP_PROJECT)
    await disconnectDb()
  } catch (err) {
    console.error('rebuild-default-world failed:', err)
    // Best-effort cleanup
    try { await deleteProject(TEMP_PROJECT) } catch {}
    try { await disconnectDb() } catch {}
    process.exit(1)
  }
}

main()
