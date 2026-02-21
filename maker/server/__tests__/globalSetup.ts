import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const projectsDir = path.join(__dirname, '..', '..', 'projects')

/**
 * Vitest globalSetup: clean up stale test project files left behind by
 * interrupted test runs. Runs once before any test file.
 */
export function setup() {
  if (!fs.existsSync(projectsDir)) return

  const entries = fs.readdirSync(projectsDir)
  for (const entry of entries) {
    // Test projects: test_*, proj_create_test_*, fork_*, del_test_*
    const isTestArtifact =
      entry.startsWith('test_') ||
      entry.startsWith('proj_create_test_') ||
      entry.startsWith('fork_') ||
      entry.startsWith('del_test_')
    if (!isTestArtifact) continue

    const fullPath = path.join(projectsDir, entry)
    const stat = fs.statSync(fullPath)
    if (stat.isDirectory()) {
      fs.rmSync(fullPath, { recursive: true, force: true })
    } else {
      fs.unlinkSync(fullPath)
    }
  }
}
