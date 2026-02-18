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
import ItemEditor from '../pages/ItemEditor'

const mockApi = vi.mocked(api)

describe('ItemEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.get.mockResolvedValue([])
  })

  it('shows weapon fields when type is weapon', async () => {
    const user = userEvent.setup()
    render(<ItemEditor />)

    await user.click(screen.getByText('+ New Item'))

    // Equipment fields should not be visible initially
    expect(screen.queryByText('Slot')).not.toBeInTheDocument()
    expect(screen.queryByText('Damage Bonus')).not.toBeInTheDocument()

    // Select weapon type
    await user.selectOptions(screen.getByRole('combobox'), 'weapon')

    expect(screen.getByText('Slot')).toBeInTheDocument()
    expect(screen.getByText('Damage Bonus')).toBeInTheDocument()
    expect(screen.getByText('Damage Range')).toBeInTheDocument()
    expect(screen.getByText('Armor Value')).toBeInTheDocument()
    expect(screen.getByText('Attack Sound')).toBeInTheDocument()
  })

  it('shows consumable fields when type is consumable', async () => {
    const user = userEvent.setup()
    render(<ItemEditor />)

    await user.click(screen.getByText('+ New Item'))
    await user.selectOptions(screen.getByRole('combobox'), 'consumable')

    expect(screen.getByText('Use Effect')).toBeInTheDocument()
    expect(screen.getByText('Use Sound')).toBeInTheDocument()
    // Equipment fields should NOT be visible
    expect(screen.queryByText('Slot')).not.toBeInTheDocument()
    expect(screen.queryByText('Damage Bonus')).not.toBeInTheDocument()
  })

  it('shows stackable fields for non-equipment types', async () => {
    const user = userEvent.setup()
    render(<ItemEditor />)

    await user.click(screen.getByText('+ New Item'))

    // Default empty type is not equipment, so stackable should show
    expect(screen.getByText('Stackable')).toBeInTheDocument()
    expect(screen.getByText('Max Stack')).toBeInTheDocument()

    // Select weapon — stackable should disappear
    await user.selectOptions(screen.getByRole('combobox'), 'weapon')
    expect(screen.queryByText('Stackable')).not.toBeInTheDocument()
  })

  it('hides equipment fields for misc type', async () => {
    const user = userEvent.setup()
    render(<ItemEditor />)

    await user.click(screen.getByText('+ New Item'))
    await user.selectOptions(screen.getByRole('combobox'), 'misc')

    expect(screen.queryByText('Slot')).not.toBeInTheDocument()
    expect(screen.queryByText('Damage Bonus')).not.toBeInTheDocument()
    expect(screen.queryByText('Use Effect')).not.toBeInTheDocument()
    // Stackable should show
    expect(screen.getByText('Stackable')).toBeInTheDocument()
  })
})
