import type { Request, Response, NextFunction } from 'express'

/**
 * Middleware that rejects mutations on read-only projects.
 * Must be mounted AFTER projectMiddleware which sets req.readOnly.
 */
export function rejectIfReadOnly(req: Request, res: Response, next: NextFunction): void {
  if (req.readOnly) {
    res.status(403).json({ error: 'This project is read-only. Fork it to make changes.' })
    return
  }
  next()
}
