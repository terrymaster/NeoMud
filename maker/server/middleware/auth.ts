import type { Request, Response, NextFunction } from 'express'
import jwt from 'jsonwebtoken'

const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-change-me-minimum-thirty-two-characters'
const JWT_PUBLIC_KEY = process.env.JWT_PUBLIC_KEY || null
const MAKER_DEV_USER_ID = process.env.MAKER_DEV_USER_ID || null

// Fail fast if dev bypass is set in production
if (process.env.NODE_ENV === 'production' && MAKER_DEV_USER_ID) {
  console.error('FATAL: MAKER_DEV_USER_ID must not be set in production')
  process.exit(1)
}

interface TokenPayload {
  userId: string
  role: string
}

function getVerifyConfig(): { key: jwt.Secret; algorithms: jwt.Algorithm[] } {
  if (JWT_PUBLIC_KEY) {
    return {
      key: Buffer.from(JWT_PUBLIC_KEY, 'base64'),
      algorithms: ['RS256'],
    }
  }
  return {
    key: JWT_SECRET,
    algorithms: ['HS256'],
  }
}

/**
 * JWT authentication middleware for the Maker.
 * Validates Platform-issued JWTs. In dev mode with MAKER_DEV_USER_ID set,
 * allows unauthenticated requests with a synthetic user.
 */
export function authenticate(req: Request, res: Response, next: NextFunction): void {
  const authHeader = req.headers.authorization

  // Dev mode bypass (only when NODE_ENV is explicitly 'development')
  if (!authHeader && MAKER_DEV_USER_ID && process.env.NODE_ENV === 'development') {
    req.user = { userId: MAKER_DEV_USER_ID, role: 'CREATOR' }
    next()
    return
  }

  if (!authHeader?.startsWith('Bearer ')) {
    res.status(401).json({ error: 'Authentication required' })
    return
  }

  const token = authHeader.slice(7)
  try {
    const { key, algorithms } = getVerifyConfig()
    const decoded = jwt.verify(token, key, {
      algorithms,
      issuer: 'neomud-platform',
    }) as TokenPayload

    req.user = { userId: decoded.userId, role: decoded.role }
    next()
  } catch {
    res.status(401).json({ error: 'Authentication required' })
  }
}
