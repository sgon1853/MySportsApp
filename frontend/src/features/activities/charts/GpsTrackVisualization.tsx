import { useMemo } from 'react'
import type { LatLngExpression } from 'leaflet'
import type { ActivityDetail } from '../../../api/types'
import { buildChartPoints } from './chartData'
import { ElevationChart } from './ElevationChart'
import { GpsTrackMap } from './GpsTrackMap'
import { HeartRateChart } from './HeartRateChart'

export function GpsTrackVisualization({ activity }: { activity: ActivityDetail }) {
  const positions = useMemo<LatLngExpression[]>(
    () =>
      activity.trackPoints
        .filter((point) => point.lat !== null && point.lon !== null)
        .map((point) => [point.lat as number, point.lon as number]),
    [activity.trackPoints],
  )

  const chartPoints = useMemo(() => buildChartPoints(activity.trackPoints), [activity.trackPoints])

  return (
    <div className="gps-visualization">
      <section aria-label="GPS track map">
        <GpsTrackMap positions={positions} />
      </section>
      <section aria-label="Heart rate over time">
        <h3>Heart rate</h3>
        <HeartRateChart data={chartPoints} />
      </section>
      <section aria-label="Elevation over distance">
        <h3>Elevation</h3>
        <ElevationChart data={chartPoints} />
      </section>
    </div>
  )
}
