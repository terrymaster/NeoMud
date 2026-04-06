// @vitest-environment jsdom
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi, describe, it, expect } from 'vitest'
import ValidationModal from '../components/ValidationModal'

describe('ValidationModal', () => {
  it('shows success message when no errors or warnings', () => {
    render(<ValidationModal errors={[]} warnings={[]} onClose={vi.fn()} />)
    expect(screen.getByText(/[Vv]alidation passed/)).toBeInTheDocument()
  })

  it('renders error count and items', () => {
    render(
      <ValidationModal
        errors={['Missing spawnRoom', 'Invalid NPC']}
        warnings={[]}
        onClose={vi.fn()}
      />
    )
    expect(screen.getByText(/Errors \(2\)/)).toBeInTheDocument()
    expect(screen.getByText('Missing spawnRoom')).toBeInTheDocument()
    expect(screen.getByText('Invalid NPC')).toBeInTheDocument()
  })

  it('renders warning count and items', () => {
    render(
      <ValidationModal
        errors={[]}
        warnings={['Weapon missing sound', 'NPC missing loot']}
        onClose={vi.fn()}
      />
    )
    expect(screen.getByText(/Warnings \(2\)/)).toBeInTheDocument()
    expect(screen.getByText('Weapon missing sound')).toBeInTheDocument()
    expect(screen.getByText('NPC missing loot')).toBeInTheDocument()
  })

  it('renders both errors and warnings', () => {
    render(
      <ValidationModal
        errors={['Error 1']}
        warnings={['Warning 1']}
        onClose={vi.fn()}
      />
    )
    expect(screen.getByText(/Errors \(1\)/)).toBeInTheDocument()
    expect(screen.getByText(/Warnings \(1\)/)).toBeInTheDocument()
  })

  it('calls onClose when Close button clicked', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    render(<ValidationModal errors={[]} warnings={[]} onClose={onClose} />)

    await user.click(screen.getByText('Close'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('calls onClose when overlay clicked', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    const { container } = render(
      <ValidationModal errors={[]} warnings={[]} onClose={onClose} />
    )

    // Click the overlay (outermost div)
    const overlay = container.firstElementChild as HTMLElement
    await user.click(overlay)
    expect(onClose).toHaveBeenCalled()
  })

  it('shows action button when errors=0, warnings>0, and actionLabel provided', () => {
    render(
      <ValidationModal
        errors={[]}
        warnings={['Some warning']}
        onClose={vi.fn()}
        actionLabel="Package Anyway"
        onAction={vi.fn()}
      />
    )
    expect(screen.getByText('Package Anyway')).toBeInTheDocument()
  })

  it('hides action button when errors present', () => {
    render(
      <ValidationModal
        errors={['Error']}
        warnings={['Warning']}
        onClose={vi.fn()}
        actionLabel="Package Anyway"
        onAction={vi.fn()}
      />
    )
    expect(screen.queryByText('Package Anyway')).not.toBeInTheDocument()
  })

  it('hides action button when no actionLabel', () => {
    render(
      <ValidationModal
        errors={[]}
        warnings={['Warning']}
        onClose={vi.fn()}
      />
    )
    // Only Close button should be present
    const buttons = screen.getAllByRole('button')
    expect(buttons).toHaveLength(1)
    expect(buttons[0]).toHaveTextContent('Close')
  })

  it('calls onAction and onClose when action button clicked', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    const onAction = vi.fn()
    render(
      <ValidationModal
        errors={[]}
        warnings={['Warning']}
        onClose={onClose}
        actionLabel="Package Anyway"
        onAction={onAction}
      />
    )

    await user.click(screen.getByText('Package Anyway'))
    expect(onClose).toHaveBeenCalledOnce()
    expect(onAction).toHaveBeenCalledOnce()
  })
})
