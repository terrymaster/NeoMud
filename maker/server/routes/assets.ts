import { Router, Request, Response } from 'express'
import multer from 'multer'
import fs from 'fs'
import path from 'path'
import { isValidAssetPath, isPathContained } from '../middleware/validateInput.js'

export const assetMgmtRouter = Router()

const MAX_HISTORY = 5

const ALLOWED_EXTENSIONS = new Set([
  '.png', '.jpg', '.jpeg', '.webp', '.gif',  // images
  '.mp3', '.ogg', '.wav', '.webm',            // audio
  '.json',                                     // data
])

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 50 * 1024 * 1024 },
  fileFilter: (_req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase()
    if (ALLOWED_EXTENSIONS.has(ext)) {
      cb(null, true)
    } else {
      cb(new Error(`File type not allowed: ${ext}`))
    }
  },
})

function getAssetsRoot(req: Request): string {
  if (!req.projectDir) throw new Error('No project context')
  return req.projectDir
}

/** Validate and resolve an asset path. Returns the full path or sends a 400 error. */
function resolveAssetPath(assetPath: string, req: Request, res: Response): string | null {
  if (!isValidAssetPath(assetPath)) {
    res.status(400).json({ error: 'Invalid asset path' })
    return null
  }
  const root = getAssetsRoot(req)
  const fullPath = path.join(root, assetPath)
  if (!isPathContained(fullPath, root)) {
    res.status(400).json({ error: 'Invalid asset path' })
    return null
  }
  return fullPath
}

function historyDir(filePath: string): string {
  return path.join(path.dirname(filePath), '.history')
}

function historyFile(filePath: string, level: number): string {
  return path.join(historyDir(filePath), `${path.basename(filePath)}.${level}`)
}

function backupAsset(filePath: string): void {
  if (!fs.existsSync(filePath)) return
  const dir = historyDir(filePath)
  fs.mkdirSync(dir, { recursive: true })

  // Shift existing backups up (5 → drop, 4→5, 3→4, 2→3, 1→2)
  for (let i = MAX_HISTORY; i >= 2; i--) {
    const src = historyFile(filePath, i - 1)
    const dest = historyFile(filePath, i)
    if (fs.existsSync(src)) {
      fs.renameSync(src, dest)
    }
  }
  // Copy current to .1
  fs.copyFileSync(filePath, historyFile(filePath, 1))
}

function restoreAsset(filePath: string): boolean {
  const newest = historyFile(filePath, 1)
  if (!fs.existsSync(newest)) return false

  // Restore .1 to current
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.copyFileSync(newest, filePath)

  // Shift backups down (2→1, 3→2, ...)
  for (let i = 1; i < MAX_HISTORY; i++) {
    const next = historyFile(filePath, i + 1)
    const cur = historyFile(filePath, i)
    if (fs.existsSync(next)) {
      fs.renameSync(next, cur)
    } else {
      if (fs.existsSync(cur)) fs.unlinkSync(cur)
      break
    }
  }
  // Remove the last slot if it was shifted
  const last = historyFile(filePath, MAX_HISTORY)
  if (fs.existsSync(last)) fs.unlinkSync(last)

  return true
}

function clearAsset(filePath: string): void {
  if (fs.existsSync(filePath)) {
    backupAsset(filePath)
    fs.unlinkSync(filePath)
  }
}

function getHistoryDepth(filePath: string): number {
  let depth = 0
  for (let i = 1; i <= MAX_HISTORY; i++) {
    if (fs.existsSync(historyFile(filePath, i))) depth = i
    else break
  }
  return depth
}

// POST /upload — multipart with assetPath field + file
assetMgmtRouter.post('/upload', upload.single('file'), (req, res) => {
  try {
    const assetPath = req.body?.assetPath
    if (!assetPath || !req.file) {
      res.status(400).json({ error: 'assetPath and file are required' })
      return
    }
    const fullPath = resolveAssetPath(assetPath, req, res)
    if (!fullPath) return
    fs.mkdirSync(path.dirname(fullPath), { recursive: true })
    backupAsset(fullPath)
    fs.writeFileSync(fullPath, req.file.buffer)
    res.json({ ok: true, assetPath })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// POST /undo — body { assetPath }
assetMgmtRouter.post('/undo', (req, res) => {
  try {
    const { assetPath } = req.body
    if (!assetPath) {
      res.status(400).json({ error: 'assetPath is required' })
      return
    }
    const fullPath = resolveAssetPath(assetPath, req, res)
    if (!fullPath) return
    const restored = restoreAsset(fullPath)
    if (!restored) {
      res.status(404).json({ error: 'No history available' })
      return
    }
    res.json({ ok: true, assetPath })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// POST /clear — body { assetPath }
assetMgmtRouter.post('/clear', (req, res) => {
  try {
    const { assetPath } = req.body
    if (!assetPath) {
      res.status(400).json({ error: 'assetPath is required' })
      return
    }
    const fullPath = resolveAssetPath(assetPath, req, res)
    if (!fullPath) return
    clearAsset(fullPath)
    res.json({ ok: true, assetPath })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// GET /history?path=...
assetMgmtRouter.get('/history', (req, res) => {
  try {
    const assetPath = req.query.path as string
    if (!assetPath) {
      res.status(400).json({ error: 'path query parameter is required' })
      return
    }
    const fullPath = resolveAssetPath(assetPath, req, res)
    if (!fullPath) return
    const depth = getHistoryDepth(fullPath)
    res.json({ depth })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})
