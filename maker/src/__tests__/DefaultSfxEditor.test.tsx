// @vitest-environment jsdom
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../api', () => {
  return {
    default: {
      get: vi.fn().mockResolvedValue([]),
      post: vi.fn().mockResolvedValue({}),
      put: vi.fn().mockResolvedValue({}),
      del: vi.fn().mockResolvedValue({}),
    },
  }
})

import api from '../api'
import DefaultSfxEditor from '../pages/DefaultSfxEditor'

const mockApi = vi.mocked(api)

function makeSfxEntry(id: string, label: string, category: string, description: string) {
  return {
    id,
    label,
    category,
    description,
    prompt: `${label.toLowerCase()} sound effect`,
    duration: 3,
  }
}

const allEntries = [
  makeSfxEntry('dodge', 'Dodge', 'combat', 'Player/NPC dodges an attack'),
  makeSfxEntry('parry', 'Parry', 'combat', 'Player parries an attack'),
  makeSfxEntry('miss', 'Miss', 'combat', 'Generic miss fallback'),
  makeSfxEntry('backstab', 'Backstab', 'combat', 'Backstab hit'),
  makeSfxEntry('enemy_death', 'Enemy Death', 'combat', 'Generic NPC death fallback'),
  makeSfxEntry('coin_pickup', 'Coin Pickup', 'loot', 'Picking up coins'),
  makeSfxEntry('item_pickup', 'Item Pickup', 'loot', 'Picking up items'),
  makeSfxEntry('loot_drop', 'Loot Drop', 'loot', 'Loot drops on ground'),
  makeSfxEntry('potion_drink', 'Potion Drink', 'item', 'Using a potion'),
  makeSfxEntry('spell_fizzle', 'Spell Fizzle', 'magic', 'Spell fails/fizzles'),
  makeSfxEntry('footstep_cobblestone', 'Footstep: Cobblestone', 'movement', 'Room transition on cobblestone'),
  makeSfxEntry('footstep_dirt', 'Footstep: Dirt', 'movement', 'Room transition on dirt'),
  makeSfxEntry('footstep_grass', 'Footstep: Grass', 'movement', 'Room transition on grass'),
  makeSfxEntry('footstep_marble', 'Footstep: Marble', 'movement', 'Room transition on marble'),
  makeSfxEntry('footstep_splash', 'Footstep: Splash', 'movement', 'Room transition in water'),
  makeSfxEntry('footstep_wood', 'Footstep: Wood', 'movement', 'Room transition on wood'),
]

const combatEntries = allEntries.filter((e) => e.category === 'combat')
const movementEntries = allEntries.filter((e) => e.category === 'movement')

function setupMockGet(entries: typeof allEntries, ...extra: (typeof allEntries)[]) {
  let callIndex = 0
  const sets = [entries, ...extra]

  mockApi.get.mockImplementation((_path: string) => {
    const result = sets[callIndex] ?? []
    callIndex++
    return Promise.resolve(result) as any
  })
}

describe('DefaultSfxEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders category filter dropdown', async () => {
    setupMockGet([])
    render(<DefaultSfxEditor />)
    await waitFor(() => {
      expect(screen.getByText('All Categories')).toBeInTheDocument()
    })
  })

  it('shows all 16 sounds with no filter', async () => {
    setupMockGet(allEntries)
    render(<DefaultSfxEditor />)
    await waitFor(() => {
      expect(screen.getByText('16 sounds')).toBeInTheDocument()
    })
  })

  it('displays sound labels in the list', async () => {
    setupMockGet(allEntries)
    render(<DefaultSfxEditor />)
    await waitFor(() => {
      expect(screen.getByText('Dodge')).toBeInTheDocument()
      expect(screen.getByText('Parry')).toBeInTheDocument()
      expect(screen.getByText('Footstep: Wood')).toBeInTheDocument()
    })
  })

  it('displays category tags on list items', async () => {
    setupMockGet(allEntries)
    render(<DefaultSfxEditor />)
    await waitFor(() => {
      // Category tags are shown (multiple "combat" tags expected)
      const combatTags = screen.getAllByText('combat')
      expect(combatTags.length).toBeGreaterThanOrEqual(5)
    })
  })

  it('selecting category filter narrows list', async () => {
    setupMockGet(allEntries, combatEntries)

    const user = userEvent.setup()
    render(<DefaultSfxEditor />)

    await waitFor(() => expect(screen.getByText('16 sounds')).toBeInTheDocument())

    const filterSelect = screen.getByDisplayValue('All Categories') as HTMLSelectElement
    await user.selectOptions(filterSelect, 'combat')

    await waitFor(() => expect(screen.getByText('5 sounds')).toBeInTheDocument())
  })

  it('selecting movement filter shows 6 footstep entries', async () => {
    setupMockGet(allEntries, movementEntries)

    const user = userEvent.setup()
    render(<DefaultSfxEditor />)

    await waitFor(() => expect(screen.getByText('16 sounds')).toBeInTheDocument())

    await user.selectOptions(screen.getByDisplayValue('All Categories') as HTMLSelectElement, 'movement')

    await waitFor(() => expect(screen.getByText('6 sounds')).toBeInTheDocument())
  })

  it('shows empty state when nothing selected', async () => {
    setupMockGet(allEntries)
    render(<DefaultSfxEditor />)

    await waitFor(() => {
      expect(screen.getByText('Select a sound effect from the list to preview and edit its AI generation prompt.')).toBeInTheDocument()
    })
  })

  it('clicking an entry opens detail panel', async () => {
    setupMockGet(allEntries)

    const user = userEvent.setup()
    render(<DefaultSfxEditor />)

    await waitFor(() => expect(screen.getByText('16 sounds')).toBeInTheDocument())

    await user.click(screen.getByText('Dodge'))

    await waitFor(() => {
      expect(screen.getByText('ID: dodge')).toBeInTheDocument()
      expect(screen.getByText('Player/NPC dodges an attack')).toBeInTheDocument()
      expect(screen.getByText('Sound Preview')).toBeInTheDocument()
    })
  })

  it('detail panel shows description and ID for selected entry', async () => {
    setupMockGet(allEntries)

    const user = userEvent.setup()
    render(<DefaultSfxEditor />)

    await waitFor(() => expect(screen.getByText('16 sounds')).toBeInTheDocument())

    await user.click(screen.getByText('Spell Fizzle'))

    await waitFor(() => {
      expect(screen.getByText('ID: spell_fizzle')).toBeInTheDocument()
      expect(screen.getByText('Spell fails/fizzles')).toBeInTheDocument()
    })
  })

  it('detail panel has SfxPreview with sound ID input', async () => {
    setupMockGet(allEntries)

    const user = userEvent.setup()
    render(<DefaultSfxEditor />)

    await waitFor(() => expect(screen.getByText('16 sounds')).toBeInTheDocument())

    await user.click(screen.getByText('Parry'))

    await waitFor(() => {
      // The SfxPreview input should show the sound ID
      const input = screen.getByDisplayValue('parry') as HTMLInputElement
      expect(input).toBeInTheDocument()
      expect(input.readOnly).toBe(true)
    })
  })

  it('detail panel has Play button', async () => {
    setupMockGet(allEntries)

    const user = userEvent.setup()
    render(<DefaultSfxEditor />)

    await waitFor(() => expect(screen.getByText('16 sounds')).toBeInTheDocument())

    await user.click(screen.getByText('Dodge'))

    await waitFor(() => {
      expect(screen.getByText('Play')).toBeInTheDocument()
    })
  })

  it('Reset All to Defaults button is present', async () => {
    setupMockGet(allEntries)

    const user = userEvent.setup()
    render(<DefaultSfxEditor />)

    await waitFor(() => expect(screen.getByText('16 sounds')).toBeInTheDocument())

    await user.click(screen.getByText('Dodge'))

    await waitFor(() => {
      expect(screen.getByText('Reset All to Defaults')).toBeInTheDocument()
    })
  })

  it('switching selection updates detail panel', async () => {
    setupMockGet(allEntries)

    const user = userEvent.setup()
    render(<DefaultSfxEditor />)

    await waitFor(() => expect(screen.getByText('16 sounds')).toBeInTheDocument())

    // Select dodge
    await user.click(screen.getByText('Dodge'))
    await waitFor(() => expect(screen.getByText('ID: dodge')).toBeInTheDocument())

    // Switch to parry
    await user.click(screen.getByText('Parry'))
    await waitFor(() => expect(screen.getByText('ID: parry')).toBeInTheDocument())
  })
})
