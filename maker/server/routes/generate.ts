import { Router } from 'express'
import fs from 'fs'
import path from 'path'
import { getProjectsDir, getActiveProject } from '../db.js'
import { readSettings } from '../settings.js'

export const generateRouter = Router()

function getAssetsRoot(): string {
  const active = getActiveProject()
  if (!active) throw new Error('No project is open')
  return path.join(getProjectsDir(), `${active}_assets`, 'assets')
}

function backupAsset(filePath: string): void {
  if (!fs.existsSync(filePath)) return
  const dir = path.join(path.dirname(filePath), '.history')
  fs.mkdirSync(dir, { recursive: true })
  const base = path.basename(filePath)
  for (let i = 5; i >= 2; i--) {
    const src = path.join(dir, `${base}.${i - 1}`)
    const dest = path.join(dir, `${base}.${i}`)
    if (fs.existsSync(src)) fs.renameSync(src, dest)
  }
  fs.copyFileSync(filePath, path.join(dir, `${base}.1`))
}

function getProviderConfig(providerId: string) {
  const settings = readSettings()
  const builtIn = settings.providers[providerId as keyof typeof settings.providers]
  if (builtIn) return { ...builtIn, id: providerId }
  const custom = settings.customProviders.find((p) => p.id === providerId)
  if (custom) return custom
  throw new Error(`Unknown provider: ${providerId}`)
}

function snapDalleSize(w: number, h: number): string {
  const aspect = w / h
  if (aspect > 1.3) return '1792x1024'
  if (aspect < 0.77) return '1024x1792'
  return '1024x1024'
}

// POST /image
generateRouter.post('/image', async (req, res) => {
  try {
    const { prompt, style, negativePrompt, width, height, assetPath } = req.body
    if (!prompt || !assetPath) {
      res.status(400).json({ error: 'prompt and assetPath are required' })
      return
    }

    const settings = readSettings()
    const providerId = settings.imageProvider
    const provider = getProviderConfig(providerId)
    const apiUrl = provider.apiUrl
    if (!apiUrl) throw new Error(`No API URL configured for ${providerId}`)

    let imageBuffer: Buffer

    if (providerId === 'openai') {
      const fullPrompt = [style && `Style: ${style}`, prompt].filter(Boolean).join('. ')
      const size = snapDalleSize(width || 1024, height || 1024)
      const response = await fetch(`${apiUrl}/images/generations`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${provider.apiKey || ''}`,
        },
        body: JSON.stringify({
          model: 'dall-e-3',
          prompt: fullPrompt,
          size,
          response_format: 'b64_json',
          n: 1,
        }),
      })
      if (!response.ok) {
        const errText = await response.text()
        throw new Error(`OpenAI API error (${response.status}): ${errText}`)
      }
      const data: any = await response.json()
      imageBuffer = Buffer.from(data.data[0].b64_json, 'base64')
    } else {
      // Stable Diffusion (A1111/Forge) or ComfyUI — SD-compatible API
      const fullPrompt = [style, prompt].filter(Boolean).join(', ')
      const response = await fetch(`${apiUrl}/sdapi/v1/txt2img`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          prompt: fullPrompt,
          negative_prompt: negativePrompt || '',
          width: width || 1024,
          height: height || 576,
          steps: 20,
          cfg_scale: 7,
        }),
      })
      if (!response.ok) {
        const errText = await response.text()
        throw new Error(`SD API error (${response.status}): ${errText}`)
      }
      const data: any = await response.json()
      if (!data.images || !data.images[0]) throw new Error('No image returned from SD API')
      imageBuffer = Buffer.from(data.images[0], 'base64')
    }

    const fullPath = path.join(getAssetsRoot(), assetPath)
    fs.mkdirSync(path.dirname(fullPath), { recursive: true })
    backupAsset(fullPath)
    fs.writeFileSync(fullPath, imageBuffer)

    res.json({ ok: true, assetPath })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// POST /sound
generateRouter.post('/sound', async (req, res) => {
  try {
    const { prompt, duration, assetPath } = req.body
    if (!prompt || !assetPath) {
      res.status(400).json({ error: 'prompt and assetPath are required' })
      return
    }

    const settings = readSettings()
    const providerId = settings.soundProvider
    const provider = getProviderConfig(providerId)
    const apiUrl = provider.apiUrl
    if (!apiUrl) throw new Error(`No API URL configured for ${providerId}`)

    if (providerId === 'elevenlabs') {
      const response = await fetch(`${apiUrl}/sound-generation`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'xi-api-key': provider.apiKey || '',
        },
        body: JSON.stringify({
          text: prompt,
          duration_seconds: duration || 120,
          prompt_influence: 0.3,
        }),
      })
      if (!response.ok) {
        const errText = await response.text()
        throw new Error(`ElevenLabs API error (${response.status}): ${errText}`)
      }
      const audioBuffer = Buffer.from(await response.arrayBuffer())
      const fullPath = path.join(getAssetsRoot(), assetPath)
      fs.mkdirSync(path.dirname(fullPath), { recursive: true })
      backupAsset(fullPath)
      fs.writeFileSync(fullPath, audioBuffer)
      res.json({ ok: true, assetPath })
    } else {
      // Custom sound provider — treat as ElevenLabs-compatible
      const response = await fetch(`${apiUrl}/sound-generation`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(provider.apiKey ? { 'xi-api-key': provider.apiKey } : {}),
        },
        body: JSON.stringify({
          text: prompt,
          duration_seconds: duration || 120,
          prompt_influence: 0.3,
        }),
      })
      if (!response.ok) {
        const errText = await response.text()
        throw new Error(`Sound API error (${response.status}): ${errText}`)
      }
      const audioBuffer = Buffer.from(await response.arrayBuffer())
      const fullPath = path.join(getAssetsRoot(), assetPath)
      fs.mkdirSync(path.dirname(fullPath), { recursive: true })
      backupAsset(fullPath)
      fs.writeFileSync(fullPath, audioBuffer)
      res.json({ ok: true, assetPath })
    }
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})
