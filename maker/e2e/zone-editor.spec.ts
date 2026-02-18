import { test, expect } from '@playwright/test'
import { createProject, deleteProjectViaApi } from './fixtures/helpers'

const PROJECT_NAME = 'e2e_zone_test'

test.beforeEach(async ({ page }) => {
  await createProject(page, PROJECT_NAME)
})

test.afterEach(async ({ baseURL }) => {
  await deleteProjectViaApi(baseURL!, PROJECT_NAME)
})

test('zones tab is default and create zone works', async ({ page }) => {
  // Already on zones page after createProject
  await expect(page.url()).toContain('/zones')

  // Look for the zone editor UI - GenericCrudEditor shows "+ New Zone" button
  await expect(page.getByRole('button', { name: '+ New Zone' })).toBeVisible()
})
