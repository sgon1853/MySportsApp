import { expect, test } from '@playwright/test'
import { login } from './helpers'

// This spec deliberately covers only what's testable without a real Strava
// account: the local/CI compose stack has no STRAVA_CLIENT_ID/SECRET
// configured, so "Connect to Strava" should surface a clear error rather
// than silently failing or crashing. The full OAuth connect -> callback ->
// sync flow requires an actual Strava account and API app registration and
// is verified manually against a real connection - see docs/deployment.md
// and the Strava integration ADR for that process; it isn't something CI
// can exercise.
test.describe('Strava integration - not configured', () => {
  test('connecting without a registered Strava API app shows a clear error', async ({ page }) => {
    await login(page)

    await page.getByRole('link', { name: 'Strava' }).click()
    await expect(page).toHaveURL(/\/integrations\/strava$/)

    await expect(page.getByRole('button', { name: 'Connect to Strava' })).toBeVisible()
    await page.getByRole('button', { name: 'Connect to Strava' }).click()

    await expect(page.getByRole('alert')).toContainText(/not configured/i)
  })
})
