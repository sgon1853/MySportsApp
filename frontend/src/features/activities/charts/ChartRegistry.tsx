import type { ComponentType } from 'react'
import type { ActivityDetail } from '../../../api/types'
import { GpsTrackVisualization } from './GpsTrackVisualization'

interface VisualizationProps {
  activity: ActivityDetail
}

/** Maps `visualizationType` to the component that renders it. Add new
 * visualization types here without touching any existing entry or the
 * fallback below. */
export const CHART_REGISTRY: Record<string, ComponentType<VisualizationProps>> = {
  GPS_TRACK: GpsTrackVisualization,
}

function UnknownVisualization({ activity }: VisualizationProps) {
  return (
    <p className="chart-empty">
      No visualization available for this activity type yet ({activity.visualizationType}).
    </p>
  )
}

export function ActivityVisualization({ activity }: VisualizationProps) {
  const Visualization = CHART_REGISTRY[activity.visualizationType] ?? UnknownVisualization
  return <Visualization activity={activity} />
}
