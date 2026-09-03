import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { expect, test } from '@playwright/test'
import { login } from './helpers'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const SAMPLE_GPX = path.join(__dirname, 'fixtures', 'sample-run.gpx')

async function uploadSampleFile(page: import('@playwright/test').Page) {
  await page.getByRole('link', { name: 'Upload' }).click()
  await expect(page).toHaveURL(/\/upload$/)

  const providerSelect = page.getByLabel('Device / provider')
  await expect(providerSelect.getByRole('option', { name: /suunto/i })).toBeAttached()
  await providerSelect.selectOption('suunto-gpx')

  await page.getByLabel('Activity file').setInputFiles(SAMPLE_GPX)
  await page.getByRole('button', { name: 'Upload', exact: true }).click()

  return page.getByRole('region', { name: 'Upload result' })
}

test.describe('uploading the same file twice', () => {
  test('the second upload of an identical file is deduped rather than duplicated', async ({ page }) => {
    await login(page)

    const firstResult = await uploadSampleFile(page)
    await expect(firstResult).toBeVisible()
    await expect(firstResult).toContainText(/import succeeded/i)

    // Re-submit the exact same file/provider combination.
    const secondResult = await uploadSampleFile(page)
    await expect(secondResult).toBeVisible()

    // The backend is expected to recognize the already-imported records and
    // report them as deduped (recordsDeduped > 0) rather than inserted
    // again, and not fail the whole batch outright.
    const dedupedValue = secondResult.locator('dt', { hasText: 'Deduped' }).locator('xpath=following-sibling::dd[1]')
    await expect(dedupedValue).not.toHaveText('0')

    const insertedValue = secondResult
      .locator('dt', { hasText: 'Inserted' })
      .locator('xpath=following-sibling::dd[1]')
    await expect(insertedValue).toHaveText('0')
  })
})
