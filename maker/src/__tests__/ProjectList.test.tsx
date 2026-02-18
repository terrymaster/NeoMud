// @vitest-environment jsdom
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi, describe, it, expect, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'

vi.mock('../api', () => {
  return {
    default: {
      get: vi.fn().mockResolvedValue({ projects: [], active: null }),
      post: vi.fn().mockResolvedValue({}),
      put: vi.fn().mockResolvedValue({}),
      del: vi.fn().mockResolvedValue({}),
    },
  }
})

import api from '../api'
import ProjectList from '../pages/ProjectList'

const mockApi = vi.mocked(api)

function renderProjectList() {
  return render(
    <MemoryRouter>
      <ProjectList />
    </MemoryRouter>
  )
}

describe('ProjectList', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.get.mockResolvedValue({ projects: [], active: null })
  })

  it('renders empty state when no projects', async () => {
    renderProjectList()
    await waitFor(() => {
      expect(screen.getByText('No projects yet. Create one above.')).toBeInTheDocument()
    })
  })

  it('renders project list', async () => {
    mockApi.get.mockResolvedValue({
      projects: [
        { name: 'My World', readOnly: false },
        { name: '_default_world', readOnly: true },
      ],
      active: null,
    })

    renderProjectList()

    await waitFor(() => {
      expect(screen.getByText('My World')).toBeInTheDocument()
      expect(screen.getByText('_default_world')).toBeInTheDocument()
    })
  })

  it('shows read-only badge for read-only projects', async () => {
    mockApi.get.mockResolvedValue({
      projects: [{ name: '_default_world', readOnly: true }],
      active: null,
    })

    renderProjectList()

    await waitFor(() => {
      expect(screen.getByText('Read Only')).toBeInTheDocument()
      expect(screen.getByText('Fork')).toBeInTheDocument()
    })
  })

  it('hides delete button for _default_world', async () => {
    mockApi.get.mockResolvedValue({
      projects: [
        { name: '_default_world', readOnly: true },
        { name: 'custom', readOnly: false },
      ],
      active: null,
    })

    renderProjectList()

    await waitFor(() => {
      // Only one Delete button should show (for 'custom')
      const deleteButtons = screen.getAllByText('Delete')
      expect(deleteButtons).toHaveLength(1)
    })
  })

  it('submits create form', async () => {
    const user = userEvent.setup()
    mockApi.post.mockResolvedValue({})

    renderProjectList()

    await user.type(screen.getByPlaceholderText('New project name...'), 'TestProject')
    await user.click(screen.getByText('New Project'))

    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith('/projects', { name: 'TestProject' })
    })
  })

  it('delete calls api.del with confirm', async () => {
    const user = userEvent.setup()
    mockApi.get.mockResolvedValue({
      projects: [{ name: 'custom', readOnly: false }],
      active: null,
    })
    mockApi.del.mockResolvedValue({})
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    renderProjectList()

    await waitFor(() => expect(screen.getByText('custom')).toBeInTheDocument())
    await user.click(screen.getByText('Delete'))

    await waitFor(() => {
      expect(mockApi.del).toHaveBeenCalledWith('/projects/custom')
    })
  })
})
