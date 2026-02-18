import type { Page } from '@playwright/test'

export async function createProject(page: Page, name: string) {
  await page.goto('/')
  await page.getByPlaceholder('New project name...').fill(name)
  await page.getByRole('button', { name: 'New Project' }).click()
  // Wait for navigation to the project's zones page
  await page.waitForURL(`**/project/${encodeURIComponent(name)}/zones`)
}

export async function navigateToTab(page: Page, tabName: string) {
  await page.getByRole('link', { name: tabName }).click()
}

export async function deleteProjectViaApi(baseURL: string, name: string) {
  await fetch(`${baseURL}/api/projects/${encodeURIComponent(name)}`, {
    method: 'DELETE',
  }).catch(() => {})
}
