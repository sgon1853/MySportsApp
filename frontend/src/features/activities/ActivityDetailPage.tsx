import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { getActivity } from '../../api/activities'
import { getApiErrorMessage } from '../../api/client'
import { ActivityVisualization } from './charts/ChartRegistry'
import {
  formatActivityType,
  formatCalories,
  formatDate,
  formatDistance,
  formatDuration,
  formatElevation,
  formatHr,
  formatPace,
} from './formatters'

export function ActivityDetailPage() {
  const { id } = useParams<{ id: string }>()

  const activityQuery = useQuery({
    queryKey: ['activities', id],
    queryFn: () => getActivity(id as string),
    enabled: Boolean(id),
  })

  if (activityQuery.isLoading) {
    return (
      <div className="page">
        <p>Loading activity…</p>
      </div>
    )
  }

  if (activityQuery.isError) {
    return (
      <div className="page">
        <div role="alert" className="banner banner--error">
          {getApiErrorMessage(activityQuery.error, 'Could not load this activity.')}
        </div>
        <p>
          <Link to="/activities">Back to activities</Link>
        </p>
      </div>
    )
  }

  const activity = activityQuery.data
  if (!activity) return null

  return (
    <div className="page">
      <p>
        <Link to="/activities">&larr; Back to activities</Link>
      </p>
      <h1>
        {formatActivityType(activity.activityType)} &middot; {formatDate(activity.startTime)}
      </h1>

      <dl className="stat-grid stat-grid--summary">
        <div>
          <dt>Distance</dt>
          <dd>{formatDistance(activity.distanceMeters)}</dd>
        </div>
        <div>
          <dt>Duration</dt>
          <dd>{formatDuration(activity.durationSeconds)}</dd>
        </div>
        <div>
          <dt>Pace</dt>
          <dd>{formatPace(activity.durationSeconds, activity.distanceMeters)}</dd>
        </div>
        <div>
          <dt>Avg HR</dt>
          <dd>{formatHr(activity.avgHr)}</dd>
        </div>
        <div>
          <dt>Max HR</dt>
          <dd>{formatHr(activity.maxHr)}</dd>
        </div>
        <div>
          <dt>Elevation gain</dt>
          <dd>{formatElevation(activity.elevationGainMeters)}</dd>
        </div>
        <div>
          <dt>Calories</dt>
          <dd>{formatCalories(activity.calories)}</dd>
        </div>
        <div>
          <dt>Source</dt>
          <dd>{activity.sourceProviderId}</dd>
        </div>
      </dl>

      <ActivityVisualization activity={activity} />
    </div>
  )
}
