export function formatDuration(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = Math.floor(totalSeconds % 60)

  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }
  return `${minutes}:${String(seconds).padStart(2, '0')}`
}

export function formatDistance(meters: number | null): string {
  if (meters === null) return '—'
  return `${(meters / 1000).toFixed(2)} km`
}

export function formatElevation(meters: number | null): string {
  if (meters === null) return '—'
  return `${Math.round(meters)} m`
}

export function formatHr(bpm: number | null): string {
  if (bpm === null) return '—'
  return `${Math.round(bpm)} bpm`
}

export function formatCalories(cal: number | null): string {
  if (cal === null) return '—'
  return `${Math.round(cal)} kcal`
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

/** Pace as min/km, only meaningful when distance is known and positive. */
export function formatPace(durationSeconds: number, distanceMeters: number | null): string {
  if (!distanceMeters || distanceMeters <= 0) return '—'
  const km = distanceMeters / 1000
  const secPerKm = durationSeconds / km
  const minutes = Math.floor(secPerKm / 60)
  const seconds = Math.round(secPerKm % 60)
  return `${minutes}:${String(seconds).padStart(2, '0')} /km`
}

export function formatActivityType(activityType: string): string {
  return activityType
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}
