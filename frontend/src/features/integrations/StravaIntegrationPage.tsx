import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { connectStrava, getStravaStatus, syncStrava } from '../../api/strava'
import { getApiErrorMessage } from '../../api/client'
import { UploadResultSummary } from '../imports/UploadPage'
import type { FileUploadOutcome } from '../imports/UploadPage'

interface CallbackMessage {
  type: 'success' | 'error'
  text: string
}

/** Captured once, from the query params Strava's OAuth callback redirect
 * left on the URL (see StravaController#callback on the backend) - read
 * with a lazy useState initializer rather than live off searchParams, since
 * the params get cleared right after (so a page refresh doesn't re-show a
 * stale banner) and the message still needs to render after that happens. */
function readCallbackMessage(searchParams: URLSearchParams): CallbackMessage | null {
  if (searchParams.get('connected')) {
    return { type: 'success', text: 'Strava connected successfully.' }
  }
  const error = searchParams.get('stravaError')
  if (error) {
    return { type: 'error', text: `Could not connect to Strava (${error}).` }
  }
  return null
}

export function StravaIntegrationPage() {
  const queryClient = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const [callbackMessage] = useState(() => readCallbackMessage(searchParams))

  // No cache invalidation/refetch needed here, deliberately: Strava's OAuth
  // redirect is a full browser navigation (not client-side routing), so
  // this page always mounts fresh with an empty query cache after it - the
  // status query's one and only fetch already reflects whatever the
  // callback just did on the backend. (An earlier version of this tried to
  // force a refetch via invalidateQueries/refetchQueries "just in case" -
  // that was solving a problem that can't actually happen in the real
  // flow, and it raced TanStack Query's own de-duplication of a refetch
  // request against that same still-in-flight initial fetch, which is worth
  // knowing about but wasn't a problem worth working around here.)
  const statusQuery = useQuery({
    queryKey: ['strava-status'],
    queryFn: getStravaStatus,
  })

  useEffect(() => {
    if (callbackMessage) {
      setSearchParams({}, { replace: true })
    }
  }, [callbackMessage, setSearchParams])

  const connectMutation = useMutation({
    mutationFn: connectStrava,
    onSuccess: (data) => {
      window.location.href = data.authorizeUrl
    },
  })

  const syncMutation = useMutation({
    mutationFn: syncStrava,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['activities'] })
    },
  })

  // UploadResultSummary takes a per-file outcome list (see UploadPage) - a
  // Strava sync is a single outcome, wrapped the same way so the aggregated
  // stat-grid/error rendering is shared, not duplicated.
  const syncOutcomes: FileUploadOutcome[] | undefined = syncMutation.data
    ? [{ fileName: 'Strava sync', result: syncMutation.data, error: null }]
    : undefined

  return (
    <div className="page">
      <h1>Strava integration</h1>

      {callbackMessage && (
        <div
          role="alert"
          className={`banner ${callbackMessage.type === 'error' ? 'banner--error' : 'banner--success'}`}
        >
          {callbackMessage.text}
        </div>
      )}

      {statusQuery.isLoading && <p>Loading connection status…</p>}
      {statusQuery.isError && (
        <div role="alert" className="banner banner--error">
          {getApiErrorMessage(statusQuery.error, 'Could not load Strava connection status.')}
        </div>
      )}

      {statusQuery.data && !statusQuery.data.connected && (
        <div className="card">
          <p>Connect your Strava account to import your activity history.</p>
          {connectMutation.isError && (
            <div role="alert" className="banner banner--error">
              {getApiErrorMessage(connectMutation.error, 'Could not start the Strava connection.')}
            </div>
          )}
          <button type="button" onClick={() => connectMutation.mutate()} disabled={connectMutation.isPending}>
            {connectMutation.isPending ? 'Redirecting…' : 'Connect to Strava'}
          </button>
        </div>
      )}

      {statusQuery.data?.connected && (
        <div className="card">
          <p>
            Connected
            {statusQuery.data.connectedAt && ` since ${new Date(statusQuery.data.connectedAt).toLocaleDateString()}`}.
          </p>
          {syncMutation.isError && (
            <div role="alert" className="banner banner--error">
              {getApiErrorMessage(syncMutation.error, 'Sync failed.')}
            </div>
          )}
          <button type="button" onClick={() => syncMutation.mutate()} disabled={syncMutation.isPending}>
            {syncMutation.isPending ? 'Syncing…' : 'Sync now'}
          </button>
        </div>
      )}

      {syncOutcomes && <UploadResultSummary outcomes={syncOutcomes} />}
    </div>
  )
}
