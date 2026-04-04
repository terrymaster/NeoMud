import type { PrismaClient } from '../generated/prisma/client.js'

declare global {
  namespace Express {
    interface Request {
      /** Authenticated user from Platform JWT */
      user?: {
        userId: string
        role: string
      }
      /** Prisma client for the current project (set by projectMiddleware) */
      db?: PrismaClient
      /** Current project name (set by projectMiddleware) */
      projectName?: string
      /** Resolved path to project assets directory (set by projectMiddleware) */
      projectDir?: string
      /** Whether the current project is read-only (set by projectMiddleware) */
      readOnly?: boolean
    }
  }
}

export {}
