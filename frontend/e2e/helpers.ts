import type { Page } from '@playwright/test'

// These specs log in as the backend's bootstrapped admin account (created
// automatically on first backend startup from ADMIN_BOOTSTRAP_EMAIL /
// ADMIN_BOOTSTRAP_PASSWORD - see .env.example and deploy/docker-compose.yml)
// rather than requiring separate seed data. Override via E2E_USER_EMAIL /
// E2E_USER_PASSWORD if the compose stack's .env uses different values.
export const E2E_USER = {
  email: process.env.E2E_USER_EMAIL ?? 'you@example.com',
  password: process.env.E2E_USER_PASSWORD ?? 'change-me',
}

export async function login(page: Page, user = E2E_USER): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('Email').fill(user.email)
  await page.getByLabel('Password').fill(user.password)
  await page.getByRole('button', { name: 'Log in' }).click()
  await page.waitForURL('**/activities')
}
