import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { redactSettings, isMaskedKey } from '../settings.js'
import type { Settings } from '../settings.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const SETTINGS_PATH = path.resolve(__dirname, '..', '..', 'settings.json')

function makeSettings(overrides?: Partial<Settings>): Settings {
  return {
    providers: {
      'stable-diffusion': { label: 'Stable Diffusion', apiKey: 'sd-key-1234567890' },
      openai: { label: 'OpenAI', apiKey: 'sk-abc123def456ghi789' },
      elevenlabs: { label: 'ElevenLabs', apiKey: 'el-secret-key-xyz' },
    },
    customProviders: [
      { id: 'custom-1', label: 'Custom', apiKey: 'custom-key-abcdef' },
    ],
    imageProvider: 'stable-diffusion',
    soundProvider: 'elevenlabs',
    providerStatus: {},
    ...overrides,
  }
}

describe('redactSettings()', () => {
  it('masks API keys showing only last 4 characters', () => {
    const settings = makeSettings()
    const redacted = redactSettings(settings)

    expect(redacted.providers.openai.apiKey).toMatch(/^\*+i789$/)
    expect(redacted.providers.elevenlabs.apiKey).toMatch(/^\*+-xyz$/)
    expect(redacted.providers['stable-diffusion'].apiKey).toMatch(/^\*+7890$/)
  })

  it('returns empty string for empty/undefined keys', () => {
    const settings = makeSettings({
      providers: {
        'stable-diffusion': { label: 'SD', apiKey: '' },
        openai: { label: 'OAI', apiKey: undefined },
        elevenlabs: { label: 'EL' },
      },
    })
    const redacted = redactSettings(settings)

    expect(redacted.providers['stable-diffusion'].apiKey).toBe('')
    expect(redacted.providers.openai.apiKey).toBe('')
    expect(redacted.providers.elevenlabs.apiKey).toBe('')
  })

  it('returns **** for keys 4 chars or fewer', () => {
    const settings = makeSettings({
      providers: {
        'stable-diffusion': { label: 'SD', apiKey: 'abc' },
        openai: { label: 'OAI', apiKey: 'abcd' },
        elevenlabs: { label: 'EL', apiKey: 'a' },
      },
    })
    const redacted = redactSettings(settings)

    expect(redacted.providers['stable-diffusion'].apiKey).toBe('****')
    expect(redacted.providers.openai.apiKey).toBe('****')
    expect(redacted.providers.elevenlabs.apiKey).toBe('****')
  })

  it('does not modify non-key fields', () => {
    const settings = makeSettings()
    const redacted = redactSettings(settings)

    expect(redacted.providers.openai.label).toBe('OpenAI')
    expect(redacted.imageProvider).toBe('stable-diffusion')
    expect(redacted.soundProvider).toBe('elevenlabs')
  })

  it('masks custom provider keys', () => {
    const settings = makeSettings()
    const redacted = redactSettings(settings)

    expect(redacted.customProviders[0].apiKey).toMatch(/^\*+cdef$/)
    expect(redacted.customProviders[0].label).toBe('Custom')
  })
})

describe('isMaskedKey()', () => {
  it('returns true for empty string', () => {
    expect(isMaskedKey('')).toBe(true)
  })

  it('returns true for undefined', () => {
    expect(isMaskedKey(undefined)).toBe(true)
  })

  it('returns true for masked values', () => {
    expect(isMaskedKey('****')).toBe(true)
    expect(isMaskedKey('***************i789')).toBe(true)
  })

  it('returns false for real key values', () => {
    expect(isMaskedKey('sk-abc123def456ghi789')).toBe(false)
    expect(isMaskedKey('my-secret-key')).toBe(false)
  })
})
