import { Router } from 'express'
import { readSettings, writeSettings, redactSettings, isMaskedKey } from '../settings.js'
import type { Settings } from '../settings.js'

export const settingsRouter = Router()

settingsRouter.get('/', (_req, res) => {
  try {
    res.json(redactSettings(readSettings()))
  } catch (err: any) {
    console.error('[settings] GET error:', err)
    res.status(500).json({ error: 'Internal server error' })
  }
})

settingsRouter.put('/', (req, res) => {
  try {
    const incoming = req.body as Settings
    const current = readSettings()

    // Preserve existing API keys when client sends masked/empty values
    for (const pid of Object.keys(current.providers) as Array<keyof typeof current.providers>) {
      if (isMaskedKey(incoming.providers?.[pid]?.apiKey)) {
        if (incoming.providers?.[pid]) {
          incoming.providers[pid].apiKey = current.providers[pid].apiKey
        }
      }
    }
    if (incoming.customProviders) {
      for (const cp of incoming.customProviders) {
        if (isMaskedKey(cp.apiKey)) {
          const existing = current.customProviders.find(e => e.id === cp.id)
          if (existing) cp.apiKey = existing.apiKey
        }
      }
    }

    writeSettings(incoming)
    res.json({ ok: true })
  } catch (err: any) {
    console.error('[settings] PUT error:', err)
    res.status(500).json({ error: 'Internal server error' })
  }
})

settingsRouter.post('/test-provider', async (req, res) => {
  try {
    const { providerId } = req.body
    if (!providerId) {
      res.status(400).json({ ok: false, error: 'providerId is required' })
      return
    }
    const settings = readSettings()
    const builtIn = settings.providers[providerId as keyof typeof settings.providers]
    const custom = settings.customProviders.find((p) => p.id === providerId)
    const provider = builtIn || custom
    if (!provider) {
      res.status(404).json({ ok: false, error: `Unknown provider: ${providerId}` })
      return
    }
    const apiUrl = provider.apiUrl
    if (!apiUrl) {
      res.status(400).json({ ok: false, error: 'No API URL configured' })
      return
    }

    let testUrl: string
    const headers: Record<string, string> = {}

    if (providerId === 'stable-diffusion') {
      testUrl = `${apiUrl}/sdapi/v1/options`
    } else if (providerId === 'openai') {
      testUrl = `${apiUrl}/models`
      headers['Authorization'] = `Bearer ${provider.apiKey || ''}`
    } else if (providerId === 'elevenlabs') {
      testUrl = `${apiUrl}/user`
      headers['xi-api-key'] = provider.apiKey || ''
    } else {
      testUrl = apiUrl
    }

    const response = await fetch(testUrl, { headers, signal: AbortSignal.timeout(10000) })
    const status = response.ok
      ? { ok: true as const, testedAt: new Date().toISOString() }
      : { ok: false as const, error: `HTTP ${response.status}: ${response.statusText}`, testedAt: new Date().toISOString() }

    const updated = readSettings()
    updated.providerStatus[providerId] = status
    writeSettings(updated)

    res.json(status)
  } catch (err: any) {
    const pid = req.body?.providerId as string | undefined
    const status = { ok: false as const, error: err.message, testedAt: new Date().toISOString() }
    try {
      if (pid) {
        const updated = readSettings()
        updated.providerStatus[pid] = status
        writeSettings(updated)
      }
    } catch { /* ignore persistence errors */ }
    res.json(status)
  }
})
