import { test, expect } from '@playwright/test'
import { createProject, deleteProjectViaApi } from './fixtures/helpers'

const PROJECT_NAME = 'e2e_item_test'

test.beforeEach(async ({ page }) => {
  await createProject(page, PROJECT_NAME)
  await page.getByRole('link', { name: 'Items' }).click()
  await page.waitForURL(`**/items`)
})

test.afterEach(async ({ baseURL }) => {
  await deleteProjectViaApi(baseURL!, PROJECT_NAME)
})

async function fillNewItem(page: import('@playwright/test').Page, id: string) {
  await page.getByRole('button', { name: '+ New Item' }).click()
  await expect(page.getByText('New Item', { exact: true })).toBeVisible()
  // The ID label is followed by its input; use label association
  const idInput = page.locator('div').filter({ hasText: /^ID$/ }).locator('..').locator('input').first()
  await idInput.fill(id)
}

test('navigate to Items tab and create a weapon', async ({ page }) => {
  await fillNewItem(page, 'test_sword')

  // Select weapon type
  await page.locator('select').selectOption('weapon')

  // Verify conditional weapon fields appear
  await expect(page.getByText('Damage Bonus')).toBeVisible()
  await expect(page.getByText('Damage Range')).toBeVisible()

  await page.getByRole('button', { name: 'Create' }).click()

  // After creation, the editor switches to Edit mode
  await expect(page.getByText('Edit Item')).toBeVisible({ timeout: 10000 })
})

test('create a consumable with useEffect field', async ({ page }) => {
  await page.getByRole('button', { name: '+ New Item' }).click()
  await page.locator('select').selectOption('consumable')

  await expect(page.getByText('Use Effect')).toBeVisible()
  await expect(page.getByText('Use Sound')).toBeVisible()
  await expect(page.getByText('Slot')).not.toBeVisible()
})

test('edit and delete an item', async ({ page }) => {
  await fillNewItem(page, 'temp_item')
  await page.getByRole('button', { name: 'Create' }).click()

  // After creation, should switch to edit mode
  await expect(page.getByText('Edit Item')).toBeVisible({ timeout: 10000 })
  await expect(page.getByRole('button', { name: 'Delete' })).toBeVisible()

  // Delete with confirmation
  page.on('dialog', (dialog) => dialog.accept())
  await page.getByRole('button', { name: 'Delete' }).click()

  // Should show empty state
  await expect(page.getByText(/Select a item to edit/i)).toBeVisible()
})
