// @vitest-environment jsdom
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../api', () => {
  return {
    default: {
      get: vi.fn().mockResolvedValue({}),
      put: vi.fn().mockResolvedValue({ ok: true }),
      post: vi.fn().mockResolvedValue({ ok: true, testedAt: new Date().toISOString() }),
    },
  }
})

function makeDefaultSettings() {
  return {
    providers: {
      'stable-diffusion': { label: 'Stable Diffusion', apiUrl: 'http://localhost:7860', apiKey: '' },
      openai: { label: 'OpenAI', apiUrl: 'https://api.openai.com/v1', apiKey: 'sk-test' },
      elevenlabs: { label: 'ElevenLabs', apiUrl: 'https://api.elevenlabs.io/v1', apiKey: '' },
    },
    customProviders: [] as any[],
    imageProvider: 'stable-diffusion',
    soundProvider: 'elevenlabs',
    providerStatus: {} as Record<string, any>,
  }
}

import api from '../api'
import SettingsEditor from '../pages/SettingsEditor'

const mockApi = vi.mocked(api)

describe('SettingsEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.get.mockResolvedValue(makeDefaultSettings())
    mockApi.put.mockResolvedValue({ ok: true })
  })

  it('loads and renders settings from API', async () => {
    render(<SettingsEditor />)

    await waitFor(() => {
      expect(screen.getByText('Settings')).toBeInTheDocument()
    })
    expect(mockApi.get).toHaveBeenCalledWith('/settings')
    expect(screen.getByText('Built-in Providers')).toBeInTheDocument()
    expect(screen.getByText('Active Providers')).toBeInTheDocument()
  })

  it('renders built-in provider inputs', async () => {
    render(<SettingsEditor />)

    await waitFor(() => {
      expect(screen.getByDisplayValue('http://localhost:7860')).toBeInTheDocument()
    })
    expect(screen.getByDisplayValue('https://api.openai.com/v1')).toBeInTheDocument()
    expect(screen.getByDisplayValue('https://api.elevenlabs.io/v1')).toBeInTheDocument()
  })

  it('save button calls api.put with settings', async () => {
    const user = userEvent.setup()
    render(<SettingsEditor />)

    await waitFor(() => {
      expect(screen.getByText('Save Settings')).toBeInTheDocument()
    })

    await user.click(screen.getByText('Save Settings'))

    await waitFor(() => {
      expect(mockApi.put).toHaveBeenCalledWith('/settings', expect.objectContaining({
        imageProvider: 'stable-diffusion',
        soundProvider: 'elevenlabs',
      }))
    })
  })

  it('add custom provider creates a new entry', async () => {
    const user = userEvent.setup()
    render(<SettingsEditor />)

    await waitFor(() => {
      expect(screen.getByText('Add Provider')).toBeInTheDocument()
    })

    await user.click(screen.getByText('Add Provider'))

    // Should show a remove button for the new provider
    expect(screen.getByText('Remove')).toBeInTheDocument()
    // Should show the provider name input
    expect(screen.getByPlaceholderText('Provider name')).toBeInTheDocument()
  })

  it('remove custom provider deletes entry', async () => {
    const user = userEvent.setup()
    mockApi.get.mockResolvedValue({
      ...makeDefaultSettings(),
      customProviders: [
        { id: 'custom_1', label: 'My Provider', apiUrl: 'http://custom.test', apiKey: '' },
      ],
    })

    render(<SettingsEditor />)

    await waitFor(() => {
      expect(screen.getByDisplayValue('My Provider')).toBeInTheDocument()
    })

    await user.click(screen.getByText('Remove'))

    expect(screen.queryByDisplayValue('My Provider')).not.toBeInTheDocument()
  })

  it('test button calls api.post for provider', async () => {
    const user = userEvent.setup()
    mockApi.post.mockResolvedValue({ ok: true, testedAt: new Date().toISOString() })
    render(<SettingsEditor />)

    await waitFor(() => {
      expect(screen.getByText('Settings')).toBeInTheDocument()
    })

    // There are multiple "Test" buttons; click the first one (active image provider)
    const testButtons = screen.getAllByText('Test')
    await user.click(testButtons[0])

    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith('/settings/test-provider', expect.objectContaining({
        providerId: expect.any(String),
      }))
    })
  })

  it('renders nothing before settings load', () => {
    // Make API hang
    mockApi.get.mockReturnValue(new Promise(() => {}))
    const { container } = render(<SettingsEditor />)
    expect(container.innerHTML).toBe('')
  })
})
