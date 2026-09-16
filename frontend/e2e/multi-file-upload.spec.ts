import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { expect, test } from '@playwright/test'
import { login } from './helpers'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const SAMPLE_RUN_GPX = path.join(__dirname, 'fixtures', 'sample-run.gpx')
const SAMPLE_RIDE_GPX = path.join(__dirname, 'fixtures', 'sample-ride-multilap.gpx')

test.describe('uploading multiple files at once', () => {
  test('both files are imported in one submission and both appear in the activity list', async ({ page }) => {
    await login(page)

    await page.getByRole('link', { name: 'Upload' }).click()
    await expect(page).toHaveURL(/\/upload$/)

    const providerSelect = page.getByLabel('Device / provider')
    await expect(providerSelect.getByRole('option', { name: /suunto/i })).toBeAttached()
    await providerSelect.selectOption('suunto-gpx')

    await page.getByLabel(/activity file/i).setInputFiles([SAMPLE_RUN_GPX, SAMPLE_RIDE_GPX])
    await expect(page.getByText('2 files selected')).toBeVisible()

    await page.getByRole('button', { name: 'Upload 2 files' }).click()

    const result = page.getByRole('region', { name: 'Upload result' })
    await expect(result).toBeVisible()
    await expect(result).toContainText(/import succeeded|import partially succeeded/i)

    // Aggregated stats across both files, plus a per-file breakdown.
    const perFile = page.getByRole('list', { name: 'Per-file results' })
    await expect(perFile).toContainText('sample-run.gpx')
    await expect(perFile).toContainText('sample-ride-multilap.gpx')

    await page.getByRole('link', { name: 'Activities' }).click()
    await expect(page).toHaveURL(/\/activities$/)
    await expect(page.getByRole('row').filter({ hasText: 'Run' }).first()).toBeVisible()
    await expect(page.getByRole('row').filter({ hasText: 'Cycling' }).first()).toBeVisible()
  })
})
