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
import PcSpriteEditor from '../pages/PcSpriteEditor'

const mockApi = vi.mocked(api)

function makeSpriteRow(race: string, gender: string, cls: string) {
  const id = `${race.toLowerCase()}_${gender}_${cls.toLowerCase()}`
  return {
    id,
    race,
    gender,
    characterClass: cls,
    imagePrompt: `A ${gender} ${race.toLowerCase()} ${cls.toLowerCase()}, fantasy RPG character portrait`,
    imageStyle: 'pixel art, 16-bit JRPG style',
    imageNegativePrompt: 'blurry',
    imageWidth: 384,
    imageHeight: 512,
  }
}

const RACES = ['HUMAN', 'DWARF', 'ELF', 'HALFLING', 'GNOME', 'HALF_ORC']
const GENDERS = ['male', 'female', 'neutral']
const CLASSES = [
  'WARRIOR', 'PALADIN', 'WITCHHUNTER', 'CLERIC', 'PRIEST', 'MISSIONARY',
  'MAGE', 'WARLOCK', 'DRUID', 'RANGER', 'THIEF', 'NINJA', 'MYSTIC', 'BARD', 'GYPSY',
]

function makeAll270(): ReturnType<typeof makeSpriteRow>[] {
  const all = []
  for (const race of RACES) {
    for (const gender of GENDERS) {
      for (const cls of CLASSES) {
        all.push(makeSpriteRow(race, gender, cls))
      }
    }
  }
  return all
}

function makeHumanSprites(): ReturnType<typeof makeSpriteRow>[] {
  const all = []
  for (const gender of GENDERS) {
    for (const cls of CLASSES) {
      all.push(makeSpriteRow('HUMAN', gender, cls))
    }
  }
  return all
}

describe('PcSpriteEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.get.mockResolvedValue([])
  })

  it('renders race, gender, and class dropdown filters', async () => {
    render(<PcSpriteEditor />)
    await waitFor(() => {
      expect(screen.getByText('All Races')).toBeInTheDocument()
      expect(screen.getByText('All Genders')).toBeInTheDocument()
      expect(screen.getByText('All Classes')).toBeInTheDocument()
    })
  })

  it('shows all 270 sprites with no filter selected', async () => {
    const all270 = makeAll270()
    mockApi.get.mockResolvedValue(all270)

    render(<PcSpriteEditor />)

    await waitFor(() => {
      expect(screen.getByText('270 sprites')).toBeInTheDocument()
    })
  })

  it('selecting race dropdown filters grid', async () => {
    const humanSprites = makeHumanSprites()
    mockApi.get
      .mockResolvedValueOnce(makeAll270()) // initial load
      .mockResolvedValueOnce(humanSprites) // after race filter

    const user = userEvent.setup()
    render(<PcSpriteEditor />)

    await waitFor(() => {
      expect(screen.getByText('270 sprites')).toBeInTheDocument()
    })

    const raceSelect = screen.getByDisplayValue('All Races') as HTMLSelectElement
    await user.selectOptions(raceSelect, 'HUMAN')

    await waitFor(() => {
      expect(screen.getByText('45 sprites')).toBeInTheDocument()
    })
  })

  it('combining race and gender filters narrows results', async () => {
    const humanMale = CLASSES.map((cls) => makeSpriteRow('HUMAN', 'male', cls))
    mockApi.get
      .mockResolvedValueOnce(makeAll270())
      .mockResolvedValueOnce(makeHumanSprites()) // race=HUMAN
      .mockResolvedValueOnce(humanMale)          // race=HUMAN&gender=male

    const user = userEvent.setup()
    render(<PcSpriteEditor />)

    await waitFor(() => {
      expect(screen.getByText('270 sprites')).toBeInTheDocument()
    })

    await user.selectOptions(screen.getByDisplayValue('All Races') as HTMLSelectElement, 'HUMAN')
    await waitFor(() => expect(screen.getByText('45 sprites')).toBeInTheDocument())

    await user.selectOptions(screen.getByDisplayValue('All Genders') as HTMLSelectElement, 'male')
    await waitFor(() => expect(screen.getByText('15 sprites')).toBeInTheDocument())
  })

  it('combining all three filters shows single sprite', async () => {
    const single = [makeSpriteRow('HUMAN', 'male', 'WARRIOR')]
    mockApi.get
      .mockResolvedValueOnce(makeAll270())
      .mockResolvedValueOnce(makeHumanSprites())
      .mockResolvedValueOnce(CLASSES.map((cls) => makeSpriteRow('HUMAN', 'male', cls)))
      .mockResolvedValueOnce(single)

    const user = userEvent.setup()
    render(<PcSpriteEditor />)

    await waitFor(() => expect(screen.getByText('270 sprites')).toBeInTheDocument())

    await user.selectOptions(screen.getByDisplayValue('All Races') as HTMLSelectElement, 'HUMAN')
    await waitFor(() => expect(screen.getByText('45 sprites')).toBeInTheDocument())

    await user.selectOptions(screen.getByDisplayValue('All Genders') as HTMLSelectElement, 'male')
    await waitFor(() => expect(screen.getByText('15 sprites')).toBeInTheDocument())

    await user.selectOptions(screen.getByDisplayValue('All Classes') as HTMLSelectElement, 'WARRIOR')
    await waitFor(() => expect(screen.getByText('1 sprites')).toBeInTheDocument())
  })

  it('clicking a sprite card opens edit panel with ImagePreview', async () => {
    const sprites = [makeSpriteRow('HUMAN', 'male', 'WARRIOR')]
    mockApi.get.mockResolvedValue(sprites)

    const user = userEvent.setup()
    render(<PcSpriteEditor />)

    await waitFor(() => expect(screen.getByText('1 sprites')).toBeInTheDocument())

    // Click the sprite card
    const card = screen.getByText('Human Male Warrior')
    await user.click(card)

    // Edit panel shows
    await waitFor(() => {
      expect(screen.getByText('ID: human_male_warrior')).toBeInTheDocument()
      expect(screen.getByText('Image Prompt')).toBeInTheDocument()
    })
  })

  it('editing prompt fires PUT via onUpdate', async () => {
    const sprite = makeSpriteRow('HUMAN', 'male', 'WARRIOR')
    mockApi.get.mockResolvedValue([sprite])
    mockApi.put.mockResolvedValue({ ...sprite, imagePrompt: sprite.imagePrompt + 'X' })

    const user = userEvent.setup()
    render(<PcSpriteEditor />)

    await waitFor(() => expect(screen.getByText('1 sprites')).toBeInTheDocument())

    await user.click(screen.getByText('Human Male Warrior'))

    await waitFor(() => expect(screen.getByText('ID: human_male_warrior')).toBeInTheDocument())

    // The prompt textarea should have the initial value — type one char to trigger onUpdate
    const promptTextarea = screen.getByDisplayValue(sprite.imagePrompt)
    await user.type(promptTextarea, 'X')

    await waitFor(() => {
      expect(mockApi.put).toHaveBeenCalledWith(
        '/pc-sprites/human_male_warrior',
        expect.objectContaining({ imagePrompt: sprite.imagePrompt + 'X' })
      )
    })
  })

  it('Copy All Prompts button is rendered and clickable', async () => {
    const sprites = [
      makeSpriteRow('HUMAN', 'male', 'WARRIOR'),
      makeSpriteRow('HUMAN', 'male', 'MAGE'),
    ]
    mockApi.get.mockResolvedValue(sprites)

    const user = userEvent.setup()
    render(<PcSpriteEditor />)

    await waitFor(() => expect(screen.getByText('2 sprites')).toBeInTheDocument())

    const btn = screen.getByText('Copy All Prompts')
    expect(btn).toBeInTheDocument()
    // Click doesn't throw even without clipboard API
    await user.click(btn)
  })
})
