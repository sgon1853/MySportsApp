import type { TrackPoint } from '../../../api/types'

const EARTH_RADIUS_METERS = 6371000

function toRadians(deg: number): number {
  return (deg * Math.PI) / 180
}

function haversineMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const dLat = toRadians(lat2 - lat1)
  const dLon = toRadians(lon2 - lon1)
  const a =
    Math.sin(dLat / 2) ** 2 + Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.sin(dLon / 2) ** 2
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return EARTH_RADIUS_METERS * c
}

export interface ChartPoint {
  elapsedSeconds: number
  distanceKm: number
  heartRate: number | null
  elevationMeters: number | null
}

/** Derives elapsed-time and cumulative-distance series from raw track
 * points, so the individual chart components can stay dumb renderers. Points
 * missing lat/lon simply don't advance the distance accumulator. */
export function buildChartPoints(trackPoints: TrackPoint[]): ChartPoint[] {
  if (trackPoints.length === 0) return []

  const startMs = new Date(trackPoints[0].timestamp).getTime()
  let cumulativeMeters = 0
  let lastLat: number | null = null
  let lastLon: number | null = null

  return trackPoints.map((point) => {
    if (point.lat !== null && point.lon !== null) {
      if (lastLat !== null && lastLon !== null) {
        cumulativeMeters += haversineMeters(lastLat, lastLon, point.lat, point.lon)
      }
      lastLat = point.lat
      lastLon = point.lon
    }

    const elapsedSeconds = (new Date(point.timestamp).getTime() - startMs) / 1000

    return {
      elapsedSeconds: Number.isFinite(elapsedSeconds) ? elapsedSeconds : 0,
      distanceKm: cumulativeMeters / 1000,
      heartRate: point.heartRate,
      elevationMeters: point.elevationMeters,
    }
  })
}

export function formatElapsed(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${String(secs).padStart(2, '0')}`
}
