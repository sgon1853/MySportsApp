import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import type { ActivityDetail } from '../../../api/types'
import { ActivityVisualization, CHART_REGISTRY } from './ChartRegistry'

const baseActivity: ActivityDetail = {
  id: 'act-1',
  activityType: 'RUN',
  visualizationType: 'GPS_TRACK',
  startTime: '2026-08-20T07:15:00Z',
  durationSeconds: 1800,
  distanceMeters: 5000,
  avgHr: 150,
  maxHr: 178,
  calories: 420,
  elevationGainMeters: 45,
  sourceProviderId: 'suunto-gpx',
  // No lat/lon so the map renders its "no coordinates" fallback instead of
  // initializing a real Leaflet map (which jsdom can't fully support).
  trackPoints: [
    { timestamp: '2026-08-20T07:15:00Z', lat: null, lon: null, elevationMeters: 30, heartRate: 120 },
    { timestamp: '2026-08-20T07:16:00Z', lat: null, lon: null, elevationMeters: 32, heartRate: 140 },
  ],
}

describe('CHART_REGISTRY', () => {
  it('registers GPS_TRACK', () => {
    expect(CHART_REGISTRY.GPS_TRACK).toBeDefined()
  })
})

describe('ActivityVisualization', () => {
  it('renders the registered component for a known visualizationType', () => {
    render(<ActivityVisualization activity={baseActivity} />)

    expect(screen.getByRole('heading', { name: 'Heart rate' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Elevation' })).toBeInTheDocument()
    expect(screen.getByText(/no gps coordinates recorded/i)).toBeInTheDocument()
  })

  it('renders a fallback for an unknown visualizationType without crashing', () => {
    const unknownActivity: ActivityDetail = { ...baseActivity, visualizationType: 'INDOOR_SESSION' }

    render(<ActivityVisualization activity={unknownActivity} />)

    expect(screen.getByText(/no visualization available for this activity type yet/i)).toBeInTheDocument()
    expect(screen.getByText(/INDOOR_SESSION/)).toBeInTheDocument()
  })
})
