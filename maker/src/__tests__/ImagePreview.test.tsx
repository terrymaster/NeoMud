// @vitest-environment jsdom
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi, describe, it, expect } from 'vitest'
import ImagePreview from '../components/ImagePreview'

describe('ImagePreview', () => {
  it('shows placeholder when no image loads', () => {
    // room type with no assetPath returns empty URL, so placeholder shows
    render(<ImagePreview entityType="room" entityId="test_room" />)
    expect(screen.getByText('Missing Asset')).toBeInTheDocument()
    expect(screen.getByText('test_room')).toBeInTheDocument()
  })

  it('shows prompt fields', () => {
    render(<ImagePreview entityType="item" entityId="test_item" />)
    expect(screen.getByText('Image Prompt')).toBeInTheDocument()
    expect(screen.getByText('Style')).toBeInTheDocument()
    expect(screen.getByText('Negative Prompt')).toBeInTheDocument()
    expect(screen.getByText('Copy Prompt')).toBeInTheDocument()
  })

  it('fires onUpdate when prompt changes', async () => {
    const user = userEvent.setup()
    const onUpdate = vi.fn()

    render(
      <ImagePreview
        entityType="item"
        entityId="test_item"
        onUpdate={onUpdate}
      />
    )

    const promptTextarea = screen.getByPlaceholderText('Describe the image to generate...')
    await user.type(promptTextarea, 'A')

    expect(onUpdate).toHaveBeenCalledWith(
      expect.objectContaining({ imagePrompt: 'A' })
    )
  })

  it('displays initial prompt values from props', () => {
    render(
      <ImagePreview
        entityType="item"
        entityId="test_item"
        imagePrompt="A shiny sword"
        imageStyle="pixel art"
        imageNegativePrompt="blurry"
        imageWidth={512}
        imageHeight={512}
      />
    )

    expect(screen.getByDisplayValue('A shiny sword')).toBeInTheDocument()
    expect(screen.getByDisplayValue('pixel art')).toBeInTheDocument()
    expect(screen.getByDisplayValue('blurry')).toBeInTheDocument()
    const inputs512 = screen.getAllByDisplayValue('512')
    expect(inputs512).toHaveLength(2) // width and height
  })
})
