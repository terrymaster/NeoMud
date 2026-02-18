import { test, expect } from '@playwright/test'
import { createProject, deleteProjectViaApi } from './fixtures/helpers'

const PROJECT_NAME = 'e2e_export_test'

test.beforeEach(async ({ page }) => {
  await createProject(page, PROJECT_NAME)
})

test.afterEach(async ({ baseURL }) => {
  await deleteProjectViaApi(baseURL!, PROJECT_NAME)
})

test('export .nmd triggers download', async ({ page }) => {
  // Listen for download event
  const downloadPromise = page.waitForEvent('download')

  await page.getByRole('button', { name: 'Export .nmd' }).click()

  const download = await downloadPromise
  expect(download.suggestedFilename()).toContain('.nmd')
})
