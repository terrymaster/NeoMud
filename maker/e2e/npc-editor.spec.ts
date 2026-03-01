import { test, expect } from '@playwright/test'
import { createProject, deleteProjectViaApi } from './fixtures/helpers'

const PROJECT_NAME = 'e2e_npc_test'

const ZONE_DATA = { id: 'test_zone', name: 'Test Zone', description: 'A test zone' }

test.afterEach(async ({ baseURL }) => {
  await deleteProjectViaApi(baseURL!, PROJECT_NAME)
})

test('NPC creation disabled when no zones exist', async ({ page }) => {
  await createProject(page, PROJECT_NAME)
  await page.getByRole('link', { name: 'NPCs' }).click()
  await page.waitForURL(`**/npcs`)

  const newBtn = page.getByRole('button', { name: '+ New NPC' })
  await expect(newBtn).toBeDisabled()
  await expect(page.getByText('Create a zone first')).toBeVisible()
})

test('create and delete an NPC after zone exists', async ({ page, baseURL }) => {
  await createProject(page, PROJECT_NAME)

  // Create a zone via API
  const createRes = await page.request.post(`${baseURL}/api/zones`, { data: ZONE_DATA })
  expect(createRes.ok()).toBeTruthy()

  // Navigate away and back to NPCs so the editor re-fetches zones
  await page.getByRole('link', { name: 'Items' }).click()
  await page.waitForURL(`**/items`)
  await page.getByRole('link', { name: 'NPCs' }).click()
  await page.waitForURL(`**/npcs`)

  // Button should be enabled now
  const newBtn = page.getByRole('button', { name: '+ New NPC' })
  await expect(newBtn).toBeEnabled({ timeout: 10000 })

  await newBtn.click()
  await expect(page.getByText('New NPC', { exact: true })).toBeVisible()

  // Fill ID
  const idInput = page.locator('div').filter({ hasText: /^ID$/ }).locator('..').locator('input').first()
  await idInput.fill('test_guard')

  // Select zone
  const zoneSelect = page.locator('select').first()
  await expect(zoneSelect.locator('option[value="test_zone"]')).toBeAttached({ timeout: 5000 })
  await zoneSelect.selectOption('test_zone')

  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page.getByText('Edit NPC')).toBeVisible({ timeout: 10000 })

  // Delete
  page.on('dialog', (dialog) => dialog.accept())
  await page.getByRole('button', { name: 'Delete' }).click()
  await expect(page.getByText(/Select an npc to edit/i)).toBeVisible()
})

test('NPC creation blocked without selecting zone', async ({ page, baseURL }) => {
  await createProject(page, PROJECT_NAME)

  const createRes = await page.request.post(`${baseURL}/api/zones`, { data: ZONE_DATA })
  expect(createRes.ok()).toBeTruthy()

  await page.getByRole('link', { name: 'Items' }).click()
  await page.waitForURL(`**/items`)
  await page.getByRole('link', { name: 'NPCs' }).click()
  await page.waitForURL(`**/npcs`)

  const newBtn = page.getByRole('button', { name: '+ New NPC' })
  await expect(newBtn).toBeEnabled({ timeout: 10000 })
  await newBtn.click()

  // Fill ID but leave zone as "-- Select --"
  const idInput = page.locator('div').filter({ hasText: /^ID$/ }).locator('..').locator('input').first()
  await idInput.fill('no_zone_npc')

  await page.getByRole('button', { name: 'Create' }).click()

  // Should show validation error, not a server 500
  await expect(page.getByText('Zone is required')).toBeVisible()
})
