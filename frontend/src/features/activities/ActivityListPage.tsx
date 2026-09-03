import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getActivities } from '../../api/activities'
import { getApiErrorMessage } from '../../api/client'
import type { ActivityFilters } from '../../api/types'
import { formatActivityType, formatDate, formatDistance, formatDuration, formatHr } from './formatters'

export function ActivityListPage() {
  const [typeFilter, setTypeFilter] = useState('')

  const filters: ActivityFilters = typeFilter ? { type: typeFilter } : {}

  const activitiesQuery = useQuery({
    queryKey: ['activities', filters],
    queryFn: () => getActivities(filters),
  })

  return (
    <div className="page">
      <div className="page-header">
        <h1>Activities</h1>
        <div className="filter-bar">
          <label htmlFor="type-filter">Type</label>
          <input
            id="type-filter"
            name="type"
            placeholder="e.g. RUN"
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
          />
        </div>
      </div>

      {activitiesQuery.isLoading && <p>Loading activities…</p>}
      {activitiesQuery.isError && (
        <div role="alert" className="banner banner--error">
          {getApiErrorMessage(activitiesQuery.error, 'Could not load activities.')}
        </div>
      )}

      {activitiesQuery.data && activitiesQuery.data.length === 0 && (
        <p>
          No activities yet. <Link to="/upload">Upload one</Link> to get started.
        </p>
      )}

      {activitiesQuery.data && activitiesQuery.data.length > 0 && (
        <table className="activity-table">
          <thead>
            <tr>
              <th>Type</th>
              <th>Date</th>
              <th>Duration</th>
              <th>Distance</th>
              <th>Avg HR</th>
            </tr>
          </thead>
          <tbody>
            {activitiesQuery.data.map((activity) => (
              <tr key={activity.id}>
                <td>
                  <Link to={`/activities/${activity.id}`}>{formatActivityType(activity.activityType)}</Link>
                </td>
                <td>{formatDate(activity.startTime)}</td>
                <td>{formatDuration(activity.durationSeconds)}</td>
                <td>{formatDistance(activity.distanceMeters)}</td>
                <td>{formatHr(activity.avgHr)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
