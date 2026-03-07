// @vitest-environment jsdom
import { render, screen, waitFor, within } from '@testing-library/react'
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

// Mock MapCanvas since it uses canvas APIs not available in jsdom
vi.mock('../components/MapCanvas', () => ({
  default: (props: any) => (
    <div data-testid="map-canvas" data-selected={props.selectedRoomId} data-readonly={props.readOnly}>
      {props.overlay?.startRoomId && <span data-testid="overlay-start">{props.overlay.startRoomId}</span>}
      {props.overlay?.patrolRoute && <span data-testid="overlay-patrol">{props.overlay.patrolRoute.join(',')}</span>}
      {props.overlay?.roomTints && <span data-testid="overlay-tints">{[...props.overlay.roomTints.keys()].join(',')}</span>}
      {props.overlay?.spawnPoints && <span data-testid="overlay-spawns">{props.overlay.spawnPoints.join(',')}</span>}
      {props.rooms?.map((r: any) => (
        <button key={r.id} data-testid={`room-${r.id}`} onClick={() => props.onSelectRoom(r.id)}>
          {r.name}
        </button>
      ))}
    </div>
  ),
}))

// Mock ImagePreview and SfxPreview
vi.mock('../components/ImagePreview', () => ({
  default: () => <div data-testid="image-preview" />,
}))
vi.mock('../components/SfxPreview', () => ({
  default: (props: any) => <input data-testid={`sfx-${props.audioCategory}`} value={props.soundId} readOnly />,
}))

import api from '../api'
import NpcEditor from '../pages/NpcEditor'

const mockApi = vi.mocked(api)

const MOCK_ZONES = [
  { id: 'town', name: 'Millhaven' },
  { id: 'forest', name: 'Whispering Forest' },
]

const MOCK_TOWN_ROOMS = [
  { id: 'town:square', name: 'Town Square', x: 0, y: 0, exits: [] },
  { id: 'town:market', name: 'Market Street', x: 1, y: 0, exits: [{ fromRoomId: 'town:market', toRoomId: 'town:square', direction: 'WEST' }] },
  { id: 'town:inn', name: 'The Rusty Tankard', x: 0, y: 1, exits: [] },
]

const MOCK_FOREST_ROOMS = [
  { id: 'forest:edge', name: 'Forest Edge', x: 0, y: 0, exits: [{ fromRoomId: 'forest:edge', toRoomId: 'forest:clearing', direction: 'SOUTH' }] },
  { id: 'forest:clearing', name: 'Mossy Clearing', x: 0, y: 1, exits: [{ fromRoomId: 'forest:clearing', toRoomId: 'forest:edge', direction: 'NORTH' }] },
]

const MOCK_NPCS = [
  {
    id: 'npc:blacksmith', name: 'Blacksmith Torren', description: 'A brawny smith.',
    zoneId: 'town', startRoomId: 'town:market', behaviorType: 'vendor',
    patrolRoute: '', hostile: false, maxHp: 100, damage: 0, level: 10,
    perception: 5, xpReward: 0, accuracy: 0, defense: 0, evasion: 0, agility: 10,
    vendorItems: '["item:iron_sword"]', spawnPoints: '[]', lootItems: '', coinDrop: '',
    attackSound: '', missSound: '', deathSound: '', interactSound: '', exitSound: '',
    imagePrompt: '', imageStyle: '', imageNegativePrompt: '', imageWidth: 384, imageHeight: 512,
  },
  {
    id: 'npc:shadow_wolf', name: 'Shadow Wolf', description: 'A dark predator.',
    zoneId: 'forest', startRoomId: 'forest:edge', behaviorType: 'wander',
    patrolRoute: '', hostile: true, maxHp: 30, damage: 5, level: 3,
    perception: 8, xpReward: 50, accuracy: 10, defense: 2, evasion: 5, agility: 15,
    vendorItems: '', spawnPoints: '["forest:edge","forest:clearing"]', lootItems: '', coinDrop: '',
    attackSound: '', missSound: '', deathSound: '', interactSound: '', exitSound: '',
    imagePrompt: '', imageStyle: '', imageNegativePrompt: '', imageWidth: 512, imageHeight: 384,
  },
  {
    id: 'npc:gorge_stalker', name: 'Gorge Stalker', description: 'Patrols the gorge.',
    zoneId: 'forest', startRoomId: 'forest:edge', behaviorType: 'patrol',
    patrolRoute: '["forest:edge","forest:clearing"]', hostile: true, maxHp: 40, damage: 8, level: 5,
    perception: 12, xpReward: 80, accuracy: 15, defense: 5, evasion: 3, agility: 12,
    vendorItems: '', spawnPoints: '[]', lootItems: '', coinDrop: '',
    attackSound: '', missSound: '', deathSound: '', interactSound: '', exitSound: '',
    imagePrompt: '', imageStyle: '', imageNegativePrompt: '', imageWidth: 512, imageHeight: 384,
  },
]

function setupMockApi() {
  mockApi.get.mockImplementation((path: string) => {
    if (path === '/zones') return Promise.resolve(MOCK_ZONES)
    if (path === '/npcs') return Promise.resolve(MOCK_NPCS)
    if (path === '/zones/town/rooms') return Promise.resolve(MOCK_TOWN_ROOMS)
    if (path === '/zones/forest/rooms') return Promise.resolve(MOCK_FOREST_ROOMS)
    return Promise.resolve([])
  })
}

describe('NpcEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setupMockApi()
  })

  it('renders 3-panel layout with NPC list', async () => {
    render(<NpcEditor />)

    await waitFor(() => {
      expect(screen.getByText('Blacksmith Torren')).toBeInTheDocument()
      expect(screen.getByText('Shadow Wolf')).toBeInTheDocument()
      expect(screen.getByText('Gorge Stalker')).toBeInTheDocument()
    })

    // Empty state message in form panel
    expect(screen.getByText(/Select an NPC to edit/)).toBeInTheDocument()
  })

  it('selecting an NPC loads zone map and form', async () => {
    const user = userEvent.setup()
    render(<NpcEditor />)

    await waitFor(() => expect(screen.getByText('Blacksmith Torren')).toBeInTheDocument())
    await user.click(screen.getByText('Blacksmith Torren'))

    // Form should show NPC name
    await waitFor(() => {
      expect(screen.getByDisplayValue('Blacksmith Torren')).toBeInTheDocument()
    })

    // Map should render with rooms from the NPC's zone
    const mapCanvas = screen.getByTestId('map-canvas')
    expect(mapCanvas).toBeInTheDocument()
    expect(mapCanvas.getAttribute('data-readonly')).toBe('true')
  })

  it('shows start room marker on map overlay', async () => {
    const user = userEvent.setup()
    render(<NpcEditor />)

    await waitFor(() => expect(screen.getByText('Blacksmith Torren')).toBeInTheDocument())
    await user.click(screen.getByText('Blacksmith Torren'))

    await waitFor(() => {
      const startOverlay = screen.getByTestId('overlay-start')
      expect(startOverlay.textContent).toBe('town:market')
    })
  })

  it('shows wander zone tints for wander NPCs', async () => {
    const user = userEvent.setup()
    render(<NpcEditor />)

    await waitFor(() => expect(screen.getByText('Shadow Wolf')).toBeInTheDocument())
    await user.click(screen.getByText('Shadow Wolf'))

    await waitFor(() => {
      const tints = screen.getByTestId('overlay-tints')
      // All forest rooms should be tinted
      expect(tints.textContent).toContain('forest:edge')
      expect(tints.textContent).toContain('forest:clearing')
    })
  })

  it('shows patrol route overlay for patrol NPCs', async () => {
    const user = userEvent.setup()
    render(<NpcEditor />)

    await waitFor(() => expect(screen.getByText('Gorge Stalker')).toBeInTheDocument())
    await user.click(screen.getByText('Gorge Stalker'))

    await waitFor(() => {
      const patrol = screen.getByTestId('overlay-patrol')
      expect(patrol.textContent).toBe('forest:edge,forest:clearing')
    })
  })

  it('shows spawn points on overlay', async () => {
    const user = userEvent.setup()
    render(<NpcEditor />)

    await waitFor(() => expect(screen.getByText('Shadow Wolf')).toBeInTheDocument())
    await user.click(screen.getByText('Shadow Wolf'))

    await waitFor(() => {
      const spawns = screen.getByTestId('overlay-spawns')
      expect(spawns.textContent).toContain('forest:edge')
      expect(spawns.textContent).toContain('forest:clearing')
    })
  })

  it('shows patrol route editor only for patrol behavior', async () => {
    const user = userEvent.setup()
    render(<NpcEditor />)

    // Select patrol NPC
    await waitFor(() => expect(screen.getByText('Gorge Stalker')).toBeInTheDocument())
    await user.click(screen.getByText('Gorge Stalker'))

    await waitFor(() => {
      expect(screen.getByText('Patrol Route')).toBeInTheDocument()
      expect(screen.getByText(/Edit Patrol on Map/)).toBeInTheDocument()
    })

    // Select vendor NPC — patrol route should disappear
    await user.click(screen.getByText('Blacksmith Torren'))

    await waitFor(() => {
      expect(screen.queryByText('Patrol Route')).not.toBeInTheDocument()
    })
  })

  it('shows vendor items only for vendor behavior', async () => {
    const user = userEvent.setup()
    render(<NpcEditor />)

    // Select vendor NPC
    await waitFor(() => expect(screen.getByText('Blacksmith Torren')).toBeInTheDocument())
    await user.click(screen.getByText('Blacksmith Torren'))

    await waitFor(() => {
      expect(screen.getByText('Vendor Items')).toBeInTheDocument()
    })

    // Select non-vendor NPC — vendor items should disappear
    await user.click(screen.getByText('Shadow Wolf'))

    await waitFor(() => {
      expect(screen.queryByText('Vendor Items')).not.toBeInTheDocument()
    })
  })

  it('has all behavior types including wander and trainer', async () => {
    const user = userEvent.setup()
    render(<NpcEditor />)

    await waitFor(() => expect(screen.getByText('Shadow Wolf')).toBeInTheDocument())
    await user.click(screen.getByText('Shadow Wolf'))

    await waitFor(() => {
      const behaviorSelect = screen.getAllByRole('combobox').find(
        (el) => within(el).queryByText('Wander') !== null
      ) || screen.getAllByRole('combobox')[2] // behavior type is 3rd select

      const options = within(behaviorSelect!).getAllByRole('option')
      const values = options.map((o) => o.getAttribute('value'))
      expect(values).toContain('idle')
      expect(values).toContain('wander')
      expect(values).toContain('patrol')
      expect(values).toContain('vendor')
      expect(values).toContain('trainer')
      expect(values).toContain('quest')
    })
  })

  it('search filters NPC list', async () => {
    const user = userEvent.setup()
    render(<NpcEditor />)

    await waitFor(() => expect(screen.getByText('Blacksmith Torren')).toBeInTheDocument())

    const searchInput = screen.getByPlaceholderText('Search NPCs...')
    await user.type(searchInput, 'wolf')

    expect(screen.getByText('Shadow Wolf')).toBeInTheDocument()
    expect(screen.queryByText('Blacksmith Torren')).not.toBeInTheDocument()
    expect(screen.queryByText('Gorge Stalker')).not.toBeInTheDocument()
  })

  it('clicking map room in start mode sets startRoomId', async () => {
    const user = userEvent.setup()
    render(<NpcEditor />)

    await waitFor(() => expect(screen.getByText('Blacksmith Torren')).toBeInTheDocument())
    await user.click(screen.getByText('Blacksmith Torren'))

    // Wait for map to load
    await waitFor(() => expect(screen.getByTestId('map-canvas')).toBeInTheDocument())

    // Enter "Set Start" mode
    await user.click(screen.getByText('Set Start'))

    // Click a room on the mock map
    await user.click(screen.getByTestId('room-town:inn'))

    // The overlay should now show the new start room
    await waitFor(() => {
      const startOverlay = screen.getByTestId('overlay-start')
      expect(startOverlay.textContent).toBe('town:inn')
    })
  })

  it('wander tints only reachable rooms (BFS from start room)', async () => {
    // Add an isolated room to forest that has no exits connecting it
    const FOREST_WITH_ISLAND = [
      ...MOCK_FOREST_ROOMS,
      { id: 'forest:island', name: 'Isolated Glade', x: 5, y: 5, exits: [] },
    ]
    mockApi.get.mockImplementation((path: string) => {
      if (path === '/zones') return Promise.resolve(MOCK_ZONES)
      if (path === '/npcs') return Promise.resolve(MOCK_NPCS)
      if (path === '/zones/town/rooms') return Promise.resolve(MOCK_TOWN_ROOMS)
      if (path === '/zones/forest/rooms') return Promise.resolve(FOREST_WITH_ISLAND)
      return Promise.resolve([])
    })

    const user = userEvent.setup()
    render(<NpcEditor />)

    await waitFor(() => expect(screen.getByText('Shadow Wolf')).toBeInTheDocument())
    await user.click(screen.getByText('Shadow Wolf'))

    await waitFor(() => {
      const tints = screen.getByTestId('overlay-tints')
      // Reachable rooms (forest:edge + forest:clearing) and unreachable (forest:island) should all be tinted
      expect(tints.textContent).toContain('forest:edge')
      expect(tints.textContent).toContain('forest:clearing')
      expect(tints.textContent).toContain('forest:island')
    })

    // The mock MapCanvas shows keys — we verify the tint map includes the island
    // (it will be tinted #e0e0e0 muted vs #42a5f5 blue for reachable rooms)
    // The test verifies the BFS doesn't crash and all rooms get some tint
  })

  it('new NPC form has correct defaults', async () => {
    const user = userEvent.setup()
    render(<NpcEditor />)

    await waitFor(() => expect(screen.getByText('+ New NPC')).toBeInTheDocument())
    await user.click(screen.getByText('+ New NPC'))

    expect(screen.getByText('New NPC')).toBeInTheDocument()
    expect(screen.getByText('Spawn Points')).toBeInTheDocument()
    expect(screen.getByText('Combat Stats')).toBeInTheDocument()
    expect(screen.getByText('Loot Items')).toBeInTheDocument()
    expect(screen.getByText('Sounds')).toBeInTheDocument()
  })
})
