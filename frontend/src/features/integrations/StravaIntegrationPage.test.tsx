import { describe, expect, it } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { renderWithProviders, signInAs } from '../../test/testUtils'
import { server } from '../../test/mocks/server'
import { mockUser } from '../../test/mocks/fixtures'
import { StravaIntegrationPage } from './StravaIntegrationPage'

describe('StravaIntegrationPage', () => {
  it('shows a connect button when no Strava account is connected', async () => {
    signInAs(mockUser)
    renderWithProviders(<StravaIntegrationPage />)

    expect(await screen.findByRole('button', { name: 'Connect to Strava' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Sync now' })).not.toBeInTheDocument()
  })

  it('navigating the browser to the authorize URL on connect', async () => {
    server.use(
      http.post('/api/v1/integrations/strava/connect', () =>
        HttpResponse.json({ authorizeUrl: 'https://www.strava.com/oauth/authorize?client_id=123' }),
      ),
    )

    // jsdom doesn't implement real navigation, and its Location object
    // doesn't allow redefining `href` directly (throws "Cannot redefine
    // property"). Replace window.location itself with a Proxy that forwards
    // everything to the real object except `href` writes, which real
    // browsers - and apiClient's baseURL, set up once in test/setup.ts from
    // window.location.origin - never need to see change mid-test. Always
    // restore the real object, even if an assertion below throws (an
    // earlier version of this test corrupted window.location for every test
    // that ran after it in this file on failure).
    const realLocation = window.location
    let capturedHref: string | undefined
    const locationProxy = new Proxy(realLocation, {
      set(target, prop, value) {
        if (prop === 'href') {
          capturedHref = value as string
          return true
        }
        return Reflect.set(target, prop, value)
      },
    })
    Object.defineProperty(window, 'location', { value: locationProxy, configurable: true, writable: true })

    try {
      signInAs(mockUser)
      const user = userEvent.setup()
      renderWithProviders(<StravaIntegrationPage />)

      await user.click(await screen.findByRole('button', { name: 'Connect to Strava' }))

      await waitFor(() => {
        expect(capturedHref).toBe('https://www.strava.com/oauth/authorize?client_id=123')
      })
    } finally {
      Object.defineProperty(window, 'location', { value: realLocation, configurable: true, writable: true })
    }
  })

  it('shows connected status and a sync button, and renders the aggregated result after syncing', async () => {
    server.use(
      http.get('/api/v1/integrations/strava/status', () =>
        HttpResponse.json({ connected: true, connectedAt: '2026-01-01T00:00:00Z' }),
      ),
      http.post('/api/v1/integrations/strava/sync', () =>
        HttpResponse.json({
          batchId: 'batch-strava-1',
          providerId: 'strava',
          status: 'SUCCESS',
          recordsParsed: 5,
          recordsInserted: 4,
          recordsDeduped: 1,
          recordsFailed: 0,
          errors: [],
        }),
      ),
    )

    signInAs(mockUser)
    const user = userEvent.setup()
    renderWithProviders(<StravaIntegrationPage />)

    expect(await screen.findByText(/connected since/i)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Sync now' }))

    expect(await screen.findByText(/import succeeded/i)).toBeInTheDocument()
    const stats = screen.getAllByRole('definition')
    expect(stats.map((el) => el.textContent)).toEqual(['5', '4', '1', '0'])
  })

  it('shows a success banner and refreshes status after the OAuth callback redirect', async () => {
    // Strava's OAuth redirect is a full browser navigation, so this page
    // always mounts fresh after it - one status fetch, already reflecting
    // what the backend just did, no forced refetch needed (see the
    // component's comment on why an earlier version of this tried to force
    // one and why that wasn't actually necessary or reliable).
    server.use(
      http.get('/api/v1/integrations/strava/status', () =>
        HttpResponse.json({ connected: true, connectedAt: '2026-01-01T00:00:00Z' }),
      ),
    )

    signInAs(mockUser)
    renderWithProviders(<StravaIntegrationPage />, { route: '/integrations/strava?connected=true' })

    expect(await screen.findByText(/strava connected successfully/i)).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Sync now' })).toBeInTheDocument()
  })

  it('shows an error banner when the OAuth callback reports a failure', async () => {
    signInAs(mockUser)
    renderWithProviders(<StravaIntegrationPage />, { route: '/integrations/strava?stravaError=connection_failed' })

    expect(await screen.findByText(/could not connect to strava \(connection_failed\)/i)).toBeInTheDocument()
  })
})
