/**
 * Kill any process listening on port 5173 (Vite dev server).
 * Used as a pre-hook for dev/rebuild scripts to prevent stale servers.
 * Exits cleanly (code 0) even if nothing is running on the port.
 * Cross-platform: supports Windows (netstat/taskkill) and macOS/Linux (lsof/kill).
 */
import { execSync } from 'child_process';

const PORT = 5173;

try {
  if (process.platform === 'win32') {
    const output = execSync(`netstat -ano`, { encoding: 'utf-8' });
    const pids = new Set<number>();

    for (const line of output.split('\n')) {
      // Match lines like: TCP  0.0.0.0:5173  ...  LISTENING  12345
      if (line.includes(`:${PORT}`) && line.includes('LISTENING')) {
        const parts = line.trim().split(/\s+/);
        const pid = parseInt(parts[parts.length - 1], 10);
        if (pid > 0) pids.add(pid);
      }
    }

    if (pids.size === 0) {
      console.log(`[kill-port] No process on port ${PORT}`);
    } else {
      for (const pid of pids) {
        console.log(`[kill-port] Killing PID ${pid} on port ${PORT}`);
        try {
          execSync(`taskkill /PID ${pid} /F`, { encoding: 'utf-8' });
        } catch {
          // Process may have already exited
        }
      }
      console.log(`[kill-port] Done`);
    }
  } else {
    // macOS / Linux: use lsof to find PIDs on the port
    let output: string;
    try {
      output = execSync(`lsof -ti :${PORT}`, { encoding: 'utf-8' });
    } catch {
      // lsof exits non-zero when no process found
      console.log(`[kill-port] No process on port ${PORT}`);
      process.exit(0);
    }

    const pids = output.trim().split('\n').map(s => parseInt(s, 10)).filter(p => p > 0);

    if (pids.length === 0) {
      console.log(`[kill-port] No process on port ${PORT}`);
    } else {
      for (const pid of pids) {
        console.log(`[kill-port] Killing PID ${pid} on port ${PORT}`);
        try {
          execSync(`kill -9 ${pid}`, { encoding: 'utf-8' });
        } catch {
          // Process may have already exited
        }
      }
      console.log(`[kill-port] Done`);
    }
  }
} catch {
  // Command failed or not available — nothing to kill
}
