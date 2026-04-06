// @vitest-environment jsdom
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../api', () => {
  return {
    default: {
      get: vi.fn().mockResolvedValue({ depth: 0 }),
      post: vi.fn().mockResolvedValue({ ok: true }),
      upload: vi.fn().mockResolvedValue({ ok: true }),
    },
  }
})

import api from '../api'
import SfxPreview from '../components/SfxPreview'

const mockApi = vi.mocked(api)

describe('SfxPreview', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.get.mockResolvedValue({ depth: 0 })
    mockApi.post.mockResolvedValue({ ok: true })
  })

  it('renders sound ID input and buttons', () => {
    render(<SfxPreview soundId="test_sound" onSoundIdChange={vi.fn()} />)
    expect(screen.getByDisplayValue('test_sound')).toBeInTheDocument()
    expect(screen.getByText('Play')).toBeInTheDocument()
    expect(screen.getByText('AI')).toBeInTheDocument()
    expect(screen.getByText('Up')).toBeInTheDocument()
    expect(screen.getByText('Undo')).toBeInTheDocument()
    expect(screen.getByText('X')).toBeInTheDocument()
  })

  it('Play button is disabled when no soundId', () => {
    render(<SfxPreview soundId="" onSoundIdChange={vi.fn()} />)
    expect(screen.getByText('Play')).toBeDisabled()
  })

  it('X (clear) button is disabled when no soundId', () => {
    render(<SfxPreview soundId="" onSoundIdChange={vi.fn()} />)
    expect(screen.getByText('X')).toBeDisabled()
  })

  it('Undo button is disabled when undoDepth=0', () => {
    render(<SfxPreview soundId="test" onSoundIdChange={vi.fn()} />)
    expect(screen.getByText('Undo')).toBeDisabled()
  })

  it('Undo button enabled when undoDepth > 0', async () => {
    mockApi.get.mockResolvedValue({ depth: 2 })
    render(<SfxPreview soundId="test" onSoundIdChange={vi.fn()} />)
    await waitFor(() => {
      expect(screen.getByText('Undo')).not.toBeDisabled()
    })
  })

  it('calls onSoundIdChange when input changes', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<SfxPreview soundId="" onSoundIdChange={onChange} />)

    const input = screen.getByPlaceholderText('sound_id')
    await user.type(input, 'a')
    expect(onChange).toHaveBeenCalledWith('a')
  })

  it('AI button opens popover with prompt field', async () => {
    const user = userEvent.setup()
    render(<SfxPreview soundId="test" onSoundIdChange={vi.fn()} />)

    await user.click(screen.getByText('AI'))
    expect(screen.getByText('SFX Prompt')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Describe the sound effect...')).toBeInTheDocument()
    expect(screen.getByText('Generate')).toBeInTheDocument()
    expect(screen.getByText('Cancel')).toBeInTheDocument()
  })

  it('AI popover closes on Cancel', async () => {
    const user = userEvent.setup()
    render(<SfxPreview soundId="test" onSoundIdChange={vi.fn()} />)

    await user.click(screen.getByText('AI'))
    expect(screen.getByText('SFX Prompt')).toBeInTheDocument()

    await user.click(screen.getByText('Cancel'))
    expect(screen.queryByText('SFX Prompt')).not.toBeInTheDocument()
  })

  it('AI popover pre-fills prompt from entityLabel', async () => {
    const user = userEvent.setup()
    render(<SfxPreview soundId="test" onSoundIdChange={vi.fn()} entityLabel="Dragon Attack" />)

    await user.click(screen.getByText('AI'))
    const textarea = screen.getByPlaceholderText('Describe the sound effect...')
    expect((textarea as HTMLTextAreaElement).value).toBe('Dragon Attack sound effect')
  })

  it('Generate calls API and closes popover', async () => {
    const user = userEvent.setup()
    render(
      <SfxPreview
        soundId="test"
        onSoundIdChange={vi.fn()}
        audioCategory="npcs"
        initialPrompt="A dragon roar"
      />
    )

    await user.click(screen.getByText('AI'))
    await user.click(screen.getByText('Generate'))

    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith('/generate/sound', {
        prompt: 'A dragon roar',
        duration: 5,
        assetPath: 'audio/npcs/test.mp3',
      })
    })
  })

  it('calls onPromptChange when prompt is edited', async () => {
    const user = userEvent.setup()
    const onPromptChange = vi.fn()
    render(
      <SfxPreview
        soundId="test"
        onSoundIdChange={vi.fn()}
        initialPrompt=""
        onPromptChange={onPromptChange}
      />
    )

    await user.click(screen.getByText('AI'))
    const textarea = screen.getByPlaceholderText('Describe the sound effect...')
    await user.type(textarea, 'x')

    expect(onPromptChange).toHaveBeenCalled()
  })
})
