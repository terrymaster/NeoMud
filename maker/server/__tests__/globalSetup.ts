/**
 * Vitest globalSetup: ensure process signals trigger a clean exit so that
 * the per-worker 'exit' handlers in helpers.ts can delete test project files.
 *
 * Vitest already handles SIGINT/SIGTERM gracefully, but this ensures
 * any edge cases (double Ctrl+C, etc.) still result in process.exit()
 * rather than an abrupt termination.
 */
export function setup() {
  const forceExit = () => process.exit(1)

  // On second SIGINT (user hammering Ctrl+C), force exit immediately
  // — the first SIGINT is handled by vitest's graceful shutdown
  let sigintCount = 0
  process.on('SIGINT', () => {
    sigintCount++
    if (sigintCount > 1) forceExit()
  })

  process.on('SIGTERM', forceExit)
}
