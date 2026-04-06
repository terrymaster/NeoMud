// @vitest-environment jsdom
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi, describe, it, expect, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

vi.mock('../api', () => {
  return {
    default: {
      get: vi.fn().mockResolvedValue({ errors: [], warnings: [] }),
      post: vi.fn().mockResolvedValue({}),
    },
  }
})

import api from '../api'
import MenuBar from '../components/MenuBar'

const mockApi = vi.mocked(api)

function renderMenuBar() {
  return render(
    <MemoryRouter initialEntries={['/project/test_project/zones']}>
      <Routes>
        <Route path="/project/:name/*" element={<MenuBar />} />
      </Routes>
    </MemoryRouter>
  )
}

describe('MenuBar', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.get.mockResolvedValue({ errors: [], warnings: [] })
  })

  it('renders menu buttons', () => {
    renderMenuBar()
    expect(screen.getByText('Save As')).toBeInTheDocument()
    expect(screen.getByText('Switch Project')).toBeInTheDocument()
    expect(screen.getByText('Validate')).toBeInTheDocument()
    expect(screen.getByText('Export .nmd')).toBeInTheDocument()
    expect(screen.getByText('Package .nmd')).toBeInTheDocument()
    expect(screen.getByText('Quit Server')).toBeInTheDocument()
  })

  it('Validate button calls API and shows modal on success', async () => {
    const user = userEvent.setup()
    mockApi.get.mockResolvedValue({ errors: [], warnings: [] })
    renderMenuBar()

    await user.click(screen.getByText('Validate'))

    await waitFor(() => {
      expect(mockApi.get).toHaveBeenCalledWith('/export/validate')
    })
    // Modal should appear with validation passed
    expect(screen.getByText('Validation Results')).toBeInTheDocument()
    expect(screen.getByText(/[Vv]alidation passed/)).toBeInTheDocument()
  })

  it('Validate shows errors in modal', async () => {
    const user = userEvent.setup()
    mockApi.get.mockResolvedValue({ errors: ['No spawnRoom'], warnings: [] })
    renderMenuBar()

    await user.click(screen.getByText('Validate'))

    await waitFor(() => {
      expect(screen.getByText('No spawnRoom')).toBeInTheDocument()
    })
  })

  it('Validate modal can be closed', async () => {
    const user = userEvent.setup()
    mockApi.get.mockResolvedValue({ errors: [], warnings: [] })
    renderMenuBar()

    await user.click(screen.getByText('Validate'))

    await waitFor(() => {
      expect(screen.getByText('Validation Results')).toBeInTheDocument()
    })

    await user.click(screen.getByText('Close'))
    expect(screen.queryByText('Validation Results')).not.toBeInTheDocument()
  })

  it('Package with errors shows modal with errors', async () => {
    const user = userEvent.setup()
    mockApi.get.mockResolvedValue({ errors: ['Missing spawnRoom'], warnings: [] })
    renderMenuBar()

    await user.click(screen.getByText('Package .nmd'))

    await waitFor(() => {
      expect(screen.getByText('Missing spawnRoom')).toBeInTheDocument()
    })
    // Should NOT show Package Anyway button since there are errors
    expect(screen.queryByText('Package Anyway')).not.toBeInTheDocument()
  })

  it('Package with warnings shows modal with action button', async () => {
    const user = userEvent.setup()
    mockApi.get.mockResolvedValue({ errors: [], warnings: ['Missing sound'] })
    renderMenuBar()

    await user.click(screen.getByText('Package .nmd'))

    await waitFor(() => {
      expect(screen.getByText('Missing sound')).toBeInTheDocument()
    })
    expect(screen.getByText('Package Anyway')).toBeInTheDocument()
  })

  it('Validate shows error modal on API failure', async () => {
    const user = userEvent.setup()
    mockApi.get.mockRejectedValue(new Error('Network error'))
    renderMenuBar()

    await user.click(screen.getByText('Validate'))

    await waitFor(() => {
      expect(screen.getByText('Network error')).toBeInTheDocument()
    })
  })
})
