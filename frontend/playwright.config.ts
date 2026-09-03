import { defineConfig, devices } from '@playwright/test'

// These specs are meant to run against the full docker-compose stack
// (frontend + backend + postgres), not a locally-started dev server -
// hence no `webServer` entry here. baseURL defaults to where
// deploy/docker-compose.yml publishes the nginx frontend service
// (FRONTEND_PORT, default 8081) and can be overridden via
// PLAYWRIGHT_BASE_URL (CI sets this explicitly).
export default defineConfig({
  testDir: './e2e',
  // Both specs log in as the same shared admin user and upload the same
  // fixture file against one live backend/database - they aren't isolated
  // from each other (no per-test tenant), so running them concurrently is
  // a real race (observed: one spec's upload interferes with the other's
  // dedup counts). Run the suite serially instead of adding per-test
  // tenant setup, which isn't needed for a 2-spec smoke suite.
  fullyParallel: false,
  workers: 1,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  reporter: 'html',
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:8081',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
