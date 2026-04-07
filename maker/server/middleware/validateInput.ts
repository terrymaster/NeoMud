import path from 'path'

const PROJECT_NAME_PATTERN = /^[a-zA-Z0-9_-]+$/

/**
 * Validate a project name — must be alphanumeric with hyphens/underscores only.
 * Prevents path traversal via project names.
 */
export function isValidProjectName(name: string): boolean {
  return name.length > 0 && name.length <= 100 && PROJECT_NAME_PATTERN.test(name)
}

/**
 * Validate an asset path — no traversal, no absolute paths.
 * Returns true if the path is safe to use within the assets directory.
 */
export function isValidAssetPath(assetPath: string): boolean {
  if (!assetPath || assetPath.startsWith('/') || assetPath.includes('..')) return false
  const normalized = path.normalize(assetPath)
  if (normalized.startsWith('..') || normalized.startsWith('/')) return false
  // Only allow safe characters (backslash included for Windows path.normalize)
  if (!/^[a-zA-Z0-9_/.\\\-]+$/.test(normalized)) return false
  return true
}

/**
 * Verify a resolved path is contained within the expected root directory.
 * Prevents path traversal even after normalization.
 */
export function isPathContained(resolvedPath: string, rootDir: string): boolean {
  const normalizedRoot = path.resolve(rootDir)
  const normalizedTarget = path.resolve(resolvedPath)
  return normalizedTarget.startsWith(normalizedRoot + path.sep) || normalizedTarget === normalizedRoot
}
