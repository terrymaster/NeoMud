// @vitest-environment jsdom
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi, describe, it, expect, beforeEach } from 'vitest'
import GenericCrudEditor from '../components/GenericCrudEditor'
import type { FieldConfig } from '../components/GenericCrudEditor'

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
const mockApi = vi.mocked(api)

const textField: FieldConfig = { key: 'id', label: 'ID', type: 'text' }
const textareaField: FieldConfig = { key: 'desc', label: 'Description', type: 'textarea' }
const numberField: FieldConfig = { key: 'value', label: 'Value', type: 'number' }
const checkboxField: FieldConfig = { key: 'active', label: 'Active', type: 'checkbox', placeholder: 'Is Active' }
const selectField: FieldConfig = {
  key: 'type', label: 'Type', type: 'select',
  options: [{ value: 'a', label: 'Option A' }, { value: 'b', label: 'Option B' }],
}
const jsonField: FieldConfig = { key: 'data', label: 'Data', type: 'json' }
const radioField: FieldConfig = {
  key: 'mode', label: 'Mode', type: 'radio',
  options: [{ value: 'x', label: 'Mode X' }, { value: 'y', label: 'Mode Y' }],
}

const allFields = [textField, textareaField, numberField, checkboxField, selectField, jsonField, radioField]

describe('GenericCrudEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.get.mockResolvedValue([])
  })

  it('renders all 7 field types when new item form is shown', async () => {
    const user = userEvent.setup()
    render(<GenericCrudEditor entityName="Item" apiPath="/items" fields={allFields} />)

    await user.click(screen.getByText('+ New Item'))

    expect(screen.getByText('New Item')).toBeInTheDocument()
    expect(screen.getByText('ID')).toBeInTheDocument()
    expect(screen.getByText('Description')).toBeInTheDocument()
    expect(screen.getByText('Value')).toBeInTheDocument()
    expect(screen.getByText('Is Active')).toBeInTheDocument()
    expect(screen.getByText('Type')).toBeInTheDocument()
    expect(screen.getByText('Data')).toBeInTheDocument()
    expect(screen.getByText('Mode X')).toBeInTheDocument()
    expect(screen.getByText('Mode Y')).toBeInTheDocument()
  })

  it('shows empty state message when nothing selected', () => {
    render(<GenericCrudEditor entityName="Item" apiPath="/items" fields={allFields} />)
    expect(screen.getByText(/Select a item to edit/i)).toBeInTheDocument()
  })

  it('"New" button creates empty form', async () => {
    const user = userEvent.setup()
    render(<GenericCrudEditor entityName="Item" apiPath="/items" fields={allFields} />)

    await user.click(screen.getByText('+ New Item'))

    expect(screen.getByText('New Item')).toBeInTheDocument()
    expect(screen.getByText('Create')).toBeInTheDocument()
    // Delete button should NOT appear for new items
    expect(screen.queryByText('Delete')).not.toBeInTheDocument()
  })

  it('selecting an item loads its data into the form', async () => {
    const user = userEvent.setup()
    const items = [{ id: 'sword', name: 'Iron Sword', desc: 'Sharp', value: 10, active: true, type: 'a', data: '', mode: 'x' }]
    mockApi.get.mockResolvedValue(items)

    render(<GenericCrudEditor entityName="Item" apiPath="/items" fields={allFields} />)

    await waitFor(() => expect(screen.getByText('Iron Sword')).toBeInTheDocument())
    await user.click(screen.getByText('Iron Sword'))

    expect(screen.getByText('Edit Item')).toBeInTheDocument()
    expect(screen.getByText('Save')).toBeInTheDocument()
    expect(screen.getByText('Delete')).toBeInTheDocument()
  })

  it('visibleWhen hides/shows fields based on form state', async () => {
    const user = userEvent.setup()
    const conditionalField: FieldConfig = {
      key: 'extra', label: 'Extra Field', type: 'text',
      visibleWhen: (form) => form.type === 'a',
    }
    const fields = [selectField, conditionalField]

    render(<GenericCrudEditor entityName="Item" apiPath="/items" fields={fields} />)
    await user.click(screen.getByText('+ New Item'))

    // Initially type is empty, so Extra Field should be hidden
    expect(screen.queryByText('Extra Field')).not.toBeInTheDocument()

    // Select option A
    await user.selectOptions(screen.getByRole('combobox'), 'a')
    expect(screen.getByText('Extra Field')).toBeInTheDocument()
  })

  it('invalid JSON blocks save', async () => {
    const user = userEvent.setup()
    render(<GenericCrudEditor entityName="Item" apiPath="/items" fields={[jsonField]} />)

    await user.click(screen.getByText('+ New Item'))
    await user.type(screen.getByRole('textbox'), '{{invalid json')
    await user.click(screen.getByText('Create'))

    expect(screen.getByText('Invalid JSON')).toBeInTheDocument()
    expect(mockApi.post).not.toHaveBeenCalled()
  })

  it('create calls api.post', async () => {
    const user = userEvent.setup()
    const fields = [textField]
    mockApi.post.mockResolvedValue({ id: 'new_item', name: 'new_item' })

    render(<GenericCrudEditor entityName="Item" apiPath="/items" fields={fields} />)
    await user.click(screen.getByText('+ New Item'))
    await user.type(screen.getByRole('textbox'), 'new_item')
    await user.click(screen.getByText('Create'))

    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith('/items', expect.objectContaining({ id: 'new_item' }))
    })
  })

  it('save calls api.put for existing item', async () => {
    const user = userEvent.setup()
    const items = [{ id: 'sword', name: 'Iron Sword' }]
    mockApi.get.mockResolvedValue(items)
    mockApi.put.mockResolvedValue({ id: 'sword', name: 'Steel Sword' })

    render(<GenericCrudEditor entityName="Item" apiPath="/items" fields={[textField, { key: 'name', label: 'Name', type: 'text' }]} />)

    await waitFor(() => expect(screen.getByText('Iron Sword')).toBeInTheDocument())
    await user.click(screen.getByText('Iron Sword'))
    await user.click(screen.getByText('Save'))

    await waitFor(() => {
      expect(mockApi.put).toHaveBeenCalledWith('/items/sword', expect.any(Object))
    })
  })

  it('delete calls api.del with confirm', async () => {
    const user = userEvent.setup()
    const items = [{ id: 'sword', name: 'Iron Sword' }]
    mockApi.get.mockResolvedValue(items)
    mockApi.del.mockResolvedValue({})
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    render(<GenericCrudEditor entityName="Item" apiPath="/items" fields={[textField]} />)

    await waitFor(() => expect(screen.getByText('Iron Sword')).toBeInTheDocument())
    await user.click(screen.getByText('Iron Sword'))
    await user.click(screen.getByText('Delete'))

    await waitFor(() => {
      expect(mockApi.del).toHaveBeenCalledWith('/items/sword')
    })
  })

  it('displays error when save fails', async () => {
    const user = userEvent.setup()
    mockApi.post.mockRejectedValue(new Error('Server error'))

    render(<GenericCrudEditor entityName="Item" apiPath="/items" fields={[textField]} />)
    await user.click(screen.getByText('+ New Item'))
    await user.click(screen.getByText('Create'))

    await waitFor(() => {
      expect(screen.getByText('Server error')).toBeInTheDocument()
    })
  })

  it('ID field is disabled in edit mode', async () => {
    const user = userEvent.setup()
    const items = [{ id: 'sword' }]
    mockApi.get.mockResolvedValue(items)

    render(<GenericCrudEditor entityName="Item" apiPath="/items" fields={[textField]} />)

    await waitFor(() => expect(screen.getByText('sword')).toBeInTheDocument())
    await user.click(screen.getByText('sword'))

    const idInput = screen.getByDisplayValue('sword')
    expect(idInput).toBeDisabled()
  })

  it('number field with max clamps input value', async () => {
    const user = userEvent.setup()
    const maxField: FieldConfig = { key: 'width', label: 'Width', type: 'number', max: 384 }
    mockApi.get.mockResolvedValue([])

    render(<GenericCrudEditor entityName="Item" apiPath="/items" fields={[maxField]} />)
    await user.click(screen.getByText('+ New Item'))

    const input = screen.getByRole('spinbutton')
    // The HTML max attribute should be set
    expect(input).toHaveAttribute('max', '384')
  })

  it('number field without max has no max attribute', async () => {
    const user = userEvent.setup()
    const noMaxField: FieldConfig = { key: 'value', label: 'Value', type: 'number' }
    mockApi.get.mockResolvedValue([])

    render(<GenericCrudEditor entityName="Item" apiPath="/items" fields={[noMaxField]} />)
    await user.click(screen.getByText('+ New Item'))

    const input = screen.getByRole('spinbutton')
    expect(input).not.toHaveAttribute('max')
  })

  it('passes maxWidth and maxHeight to ImagePreview', async () => {
    const items = [{ id: 'sword', name: 'Sword', imagePrompt: '', imageStyle: '', imageNegativePrompt: '', imageWidth: 512, imageHeight: 512 }]
    mockApi.get.mockResolvedValue(items)

    render(
      <GenericCrudEditor
        entityName="Item"
        apiPath="/items"
        fields={[{ key: 'name', label: 'Name', type: 'text' as const }]}
        imagePreview={{ entityType: 'item', maxWidth: 256, maxHeight: 256 }}
      />
    )

    await waitFor(() => expect(screen.getByText('Sword')).toBeInTheDocument())
    await userEvent.click(screen.getByText('Sword'))

    // ImagePreview should show max labels
    expect(screen.getByText(/Width \(max 256\)/)).toBeInTheDocument()
    expect(screen.getByText(/Height \(max 256\)/)).toBeInTheDocument()
  })
})
