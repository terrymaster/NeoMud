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
let prisma: PrismaClient | null = null

function dbUrl(projectName: string): string {
  const dbPath = path.join(projectsDir, `${projectName}.db`)
  return `file:${dbPath}`
}

export function getProjectsDir(): string {
  return projectsDir
}

export function listProjects(): string[] {
  return fs
    .readdirSync(projectsDir)
    .filter((f) => f.endsWith('.db'))
    .map((f) => f.replace('.db', ''))
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
  return prisma
}

export async function createProject(name: string): Promise<PrismaClient> {
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

  return openProject(name)
}

export async function deleteProject(name: string): Promise<void> {
  if (activeProject === name && prisma) {
    await prisma.$disconnect()
    prisma = null
    activeProject = null
  }
  const dbPath = path.join(projectsDir, `${name}.db`)
  if (fs.existsSync(dbPath)) {
    fs.unlinkSync(dbPath)
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
