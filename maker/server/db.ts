import { PrismaClient } from '@prisma/client'
import { execSync } from 'child_process'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const projectsDir = path.join(__dirname, '..', 'projects')

if (!fs.existsSync(projectsDir)) {
  fs.mkdirSync(projectsDir, { recursive: true })
}

let activeProject: string | null = null
let activeReadOnly = false
let prisma: PrismaClient | null = null

function dbUrl(projectName: string): string {
  const dbPath = path.join(projectsDir, `${projectName}.db`)
  return `file:${dbPath}`
}

export function getProjectsDir(): string {
  return projectsDir
}

export interface ProjectInfo {
  name: string
  readOnly: boolean
}

export async function listProjects(): Promise<ProjectInfo[]> {
  const names = fs
    .readdirSync(projectsDir)
    .filter((f) => f.endsWith('.db'))
    .map((f) => f.replace('.db', ''))

  const results: ProjectInfo[] = []
  for (const name of names) {
    // Check if project has readOnly meta by peeking at its DB
    const readOnly = await isProjectReadOnly(name)
    results.push({ name, readOnly })
  }
  return results
}

async function isProjectReadOnly(name: string): Promise<boolean> {
  // If it's the currently open project, use the active client
  if (name === activeProject && prisma) {
    try {
      const meta = await prisma.projectMeta.findUnique({ where: { key: 'readOnly' } })
      return meta?.value === 'true'
    } catch {
      return false
    }
  }

  // Otherwise open a temporary client to check
  const tmpClient = new PrismaClient({
    datasources: { db: { url: dbUrl(name) } },
  })
  try {
    await tmpClient.$connect()
    const meta = await tmpClient.projectMeta.findUnique({ where: { key: 'readOnly' } })
    return meta?.value === 'true'
  } catch {
    return false
  } finally {
    await tmpClient.$disconnect()
  }
}

export async function openProject(name: string): Promise<PrismaClient> {
  if (prisma) {
    await prisma.$disconnect()
  }
  activeProject = name
  prisma = new PrismaClient({
    datasources: { db: { url: dbUrl(name) } },
  })
  await prisma.$connect()

  // Cache the read-only flag
  try {
    const meta = await prisma.projectMeta.findUnique({ where: { key: 'readOnly' } })
    activeReadOnly = meta?.value === 'true'
  } catch {
    activeReadOnly = false
  }

  return prisma
}

export async function createProject(name: string, readOnly = false): Promise<PrismaClient> {
  const dbPath = path.join(projectsDir, `${name}.db`)
  if (fs.existsSync(dbPath)) {
    throw new Error(`Project "${name}" already exists`)
  }

  // Push schema to the new database
  const schemaPath = path.join(__dirname, '..', 'prisma', 'schema.prisma')
  execSync(`npx prisma db push --schema="${schemaPath}" --skip-generate`, {
    env: { ...process.env, DATABASE_URL: `file:${dbPath}` },
    cwd: path.join(__dirname, '..'),
    stdio: 'pipe',
  })

  const client = await openProject(name)

  if (readOnly) {
    await client.projectMeta.create({ data: { key: 'readOnly', value: 'true' } })
    activeReadOnly = true
  }

  return client
}

export async function deleteProject(name: string): Promise<void> {
  if (activeProject === name && prisma) {
    await prisma.$disconnect()
    prisma = null
    activeProject = null
    activeReadOnly = false
  }
  const dbPath = path.join(projectsDir, `${name}.db`)
  if (fs.existsSync(dbPath)) {
    fs.unlinkSync(dbPath)
  }
}

export async function forkProject(source: string, newName: string): Promise<void> {
  const srcPath = path.join(projectsDir, `${source}.db`)
  const destPath = path.join(projectsDir, `${newName}.db`)

  if (!fs.existsSync(srcPath)) {
    throw new Error(`Source project "${source}" not found`)
  }
  if (fs.existsSync(destPath)) {
    throw new Error(`Project "${newName}" already exists`)
  }

  // Copy the DB file
  fs.copyFileSync(srcPath, destPath)

  // Open the copy and remove the read-only flag
  const tmpClient = new PrismaClient({
    datasources: { db: { url: `file:${destPath}` } },
  })
  try {
    await tmpClient.$connect()
    await tmpClient.projectMeta.deleteMany({ where: { key: 'readOnly' } })
  } finally {
    await tmpClient.$disconnect()
  }
}

export function db(): PrismaClient {
  if (!prisma) {
    throw new Error('No project is open. Call openProject() first.')
  }
  return prisma
}

export function getActiveProject(): string | null {
  return activeProject
}

export function isReadOnly(): boolean {
  return activeReadOnly
}
