import { test, expect } from '@playwright/test'
import { createProject, deleteProjectViaApi } from './fixtures/helpers'

const PROJECT_NAME = 'e2e_test_project'

test.afterEach(async ({ baseURL }) => {
  await deleteProjectViaApi(baseURL!, PROJECT_NAME)
})

test('create project and verify redirect', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByText('NeoMUD Maker')).toBeVisible()

  await page.getByPlaceholder('New project name...').fill(PROJECT_NAME)
  await page.getByRole('button', { name: 'New Project' }).click()

  await page.waitForURL(`**/project/${PROJECT_NAME}/zones`)
  await expect(page.getByText(PROJECT_NAME)).toBeVisible()
})

test('open existing project', async ({ page }) => {
  await createProject(page, PROJECT_NAME)

  // Go back to project list
  await page.getByRole('button', { name: 'Switch Project' }).click()
  await page.waitForURL('**/')

  // Click the project name to open it
  await page.getByText(PROJECT_NAME).click()
  await page.waitForURL(`**/project/${PROJECT_NAME}/zones`)
})

test('delete project with confirmation', async ({ page }) => {
  await createProject(page, PROJECT_NAME)
  await page.getByRole('button', { name: 'Switch Project' }).click()
  await page.waitForURL('**/')

  // Find the specific list item for our project and click its Delete button
  const projectRow = page.getByRole('listitem').filter({ hasText: PROJECT_NAME })
  await expect(projectRow).toBeVisible()

  page.on('dialog', (dialog) => dialog.accept())
  await projectRow.getByRole('button', { name: 'Delete' }).click()

  await expect(projectRow).not.toBeVisible()
})
