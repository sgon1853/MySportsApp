import { apiClient } from './client'
import type { ImportBatchResult, StravaConnectResponse, StravaStatus } from './types'

export async function getStravaStatus(): Promise<StravaStatus> {
  const { data } = await apiClient.get<StravaStatus>('/v1/integrations/strava/status')
  return data
}

/** Returns the URL to navigate the browser to - the caller (not this
 * function) does `window.location.href = authorizeUrl`, since a normal
 * authenticated XHR is how the initiating half of the OAuth flow works here
 * (see StravaOAuthService's javadoc on the backend for why). */
export async function connectStrava(): Promise<StravaConnectResponse> {
  const { data } = await apiClient.post<StravaConnectResponse>('/v1/integrations/strava/connect')
  return data
}

export async function syncStrava(): Promise<ImportBatchResult> {
  const { data } = await apiClient.post<ImportBatchResult>('/v1/integrations/strava/sync')
  return data
}
