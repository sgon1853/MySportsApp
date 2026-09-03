import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { expect, test } from '@playwright/test'
import { login } from './helpers'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const SAMPLE_GPX = path.join(__dirname, 'fixtures', 'sample-run.gpx')

test.describe('upload an activity and visualize it', () => {
  test('uploading a GPX file surfaces the import result and a browsable activity with a map + charts', async ({
    page,
  }) => {
    await login(page)

    await page.getByRole('link', { name: 'Upload' }).click()
    await expect(page).toHaveURL(/\/upload$/)

    // Provider list is fetched from the API, not hardcoded - wait for the
    // real option to appear before selecting it.
    const providerSelect = page.getByLabel('Device / provider')
    await expect(providerSelect.getByRole('option', { name: /suunto/i })).toBeAttached()
    await providerSelect.selectOption('suunto-gpx')

    await page.getByLabel('Activity file').setInputFiles(SAMPLE_GPX)
    await page.getByRole('button', { name: 'Upload', exact: true }).click()

    const result = page.getByRole('region', { name: 'Upload result' })
    await expect(result).toBeVisible()
    await expect(result).toContainText(/import succeeded|import partially succeeded/i)

    // The list page's activities query should be invalidated by a
    // successful upload, so the newly imported activity shows up.
    await page.getByRole('link', { name: 'Activities' }).click()
    await expect(page).toHaveURL(/\/activities$/)

    const activityRow = page.getByRole('row').filter({ hasText: 'Run' }).first()
    await expect(activityRow).toBeVisible()
    await activityRow.getByRole('link').click()

    await expect(page).toHaveURL(/\/activities\/.+/)
    await expect(page.getByRole('region', { name: 'GPS track map' })).toBeVisible()
    await expect(page.getByRole('region', { name: 'Heart rate over time' })).toBeVisible()
    await expect(page.getByRole('region', { name: 'Elevation over distance' })).toBeVisible()

    // Leaflet renders tiles/canvas rather than accessible DOM, so assert on
    // its container instead of a role.
    await expect(page.locator('.leaflet-container')).toBeVisible()
  })
})
