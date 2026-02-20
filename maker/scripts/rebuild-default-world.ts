/**
 * Rebuild maker/default_world.nmd by zipping maker/default_world_src/.
 *
 * Usage:  cd maker && npm run rebuild-world
 *
 * This replaces the old round-trip (import → Prisma → export) with a direct
 * ZIP of the source directory, which is faster and lossless.
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import AdmZip from 'adm-zip'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const SRC_DIR = path.resolve(__dirname, '..', 'default_world_src')
const NMD_PATH = path.resolve(__dirname, '..', 'default_world.nmd')

function addDirToZip(zip: AdmZip, dirPath: string, zipPrefix: string) {
  for (const entry of fs.readdirSync(dirPath, { withFileTypes: true })) {
    const fullPath = path.join(dirPath, entry.name)
    const zipPath = zipPrefix ? `${zipPrefix}/${entry.name}` : entry.name
    if (entry.isDirectory()) {
      addDirToZip(zip, fullPath, zipPath)
    } else {
      zip.addLocalFile(fullPath, zipPrefix || '')
    }
  }
}

function main() {
  if (!fs.existsSync(SRC_DIR)) {
    console.error(`Source directory not found: ${SRC_DIR}`)
    process.exit(1)
  }

  console.log(`Zipping ${SRC_DIR} ...`)
  const zip = new AdmZip()
  addDirToZip(zip, SRC_DIR, '')

  zip.writeZip(NMD_PATH)
  const stats = fs.statSync(NMD_PATH)
  const sizeMB = (stats.size / (1024 * 1024)).toFixed(1)
  console.log(`Wrote ${sizeMB} MB to ${NMD_PATH}`)
}

main()
