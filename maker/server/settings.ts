import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const SETTINGS_PATH = path.resolve(__dirname, '..', 'settings.json')

export interface ProviderConfig {
  label: string
  apiUrl?: string
  apiKey?: string
}

export interface CustomProvider {
  id: string
  label: string
  apiUrl?: string
  apiKey?: string
}

export interface ProviderStatus {
  ok: boolean
  error?: string
  testedAt: string
}

export interface Settings {
  providers: {
    'stable-diffusion': ProviderConfig
    openai: ProviderConfig
    elevenlabs: ProviderConfig
  }
  customProviders: CustomProvider[]
  imageProvider: string
  soundProvider: string
  providerStatus: Record<string, ProviderStatus>
}

const DEFAULT_SETTINGS: Settings = {
  providers: {
    'stable-diffusion': { label: 'Stable Diffusion', apiUrl: 'http://localhost:7860', apiKey: '' },
    openai: { label: 'OpenAI', apiUrl: 'https://api.openai.com/v1', apiKey: '' },
    elevenlabs: { label: 'ElevenLabs', apiUrl: 'https://api.elevenlabs.io/v1', apiKey: '' },
  },
  customProviders: [],
  imageProvider: 'stable-diffusion',
  soundProvider: 'elevenlabs',
  providerStatus: {},
}

export function readSettings(): Settings {
  if (!fs.existsSync(SETTINGS_PATH)) {
    return structuredClone(DEFAULT_SETTINGS)
  }
  try {
    const raw = JSON.parse(fs.readFileSync(SETTINGS_PATH, 'utf-8'))
    return {
      providers: {
        'stable-diffusion': { ...DEFAULT_SETTINGS.providers['stable-diffusion'], ...raw.providers?.['stable-diffusion'] },
        openai: { ...DEFAULT_SETTINGS.providers.openai, ...raw.providers?.openai },
        elevenlabs: { ...DEFAULT_SETTINGS.providers.elevenlabs, ...raw.providers?.elevenlabs },
      },
      customProviders: Array.isArray(raw.customProviders) ? raw.customProviders : [],
      imageProvider: raw.imageProvider || DEFAULT_SETTINGS.imageProvider,
      soundProvider: raw.soundProvider || DEFAULT_SETTINGS.soundProvider,
      providerStatus: raw.providerStatus && typeof raw.providerStatus === 'object' ? raw.providerStatus : {},
    }
  } catch {
    return structuredClone(DEFAULT_SETTINGS)
  }
}

export function writeSettings(settings: Settings): void {
  fs.writeFileSync(SETTINGS_PATH, JSON.stringify(settings, null, 2))
}
