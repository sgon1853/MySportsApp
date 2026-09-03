import type { ActivityDetail, ActivitySummary, ImportProvider, User } from '../../api/types'

export const mockUser: User = {
  id: 'user-1',
  email: 'runner@example.com',
  role: 'USER',
}

export const mockAdmin: User = {
  id: 'admin-1',
  email: 'admin@example.com',
  role: 'ADMIN',
}

export const mockToken = 'mock-jwt-token'

export const mockProviders: ImportProvider[] = [
  {
    providerId: 'suunto-gpx',
    displayName: 'Suunto (GPX)',
    supportedExtensions: ['.gpx'],
  },
]

export const mockActivities: ActivitySummary[] = [
  {
    id: 'act-1',
    activityType: 'RUN',
    visualizationType: 'GPS_TRACK',
    startTime: '2026-08-20T07:15:00Z',
    durationSeconds: 1800,
    distanceMeters: 5000,
    avgHr: 152,
    maxHr: 178,
    calories: 420,
    elevationGainMeters: 45,
    sourceProviderId: 'suunto-gpx',
  },
  {
    id: 'act-2',
    activityType: 'RIDE',
    visualizationType: 'GPS_TRACK',
    startTime: '2026-08-18T17:00:00Z',
    durationSeconds: 5400,
    distanceMeters: 32000,
    avgHr: 138,
    maxHr: 165,
    calories: 890,
    elevationGainMeters: 210,
    sourceProviderId: 'suunto-gpx',
  },
]

export const mockActivityDetail: ActivityDetail = {
  ...mockActivities[0],
  trackPoints: [
    { timestamp: '2026-08-20T07:15:00Z', lat: 45.5, lon: -122.6, elevationMeters: 30, heartRate: 120 },
    { timestamp: '2026-08-20T07:20:00Z', lat: 45.51, lon: -122.61, elevationMeters: 35, heartRate: 150 },
    { timestamp: '2026-08-20T07:25:00Z', lat: 45.52, lon: -122.62, elevationMeters: 40, heartRate: 160 },
  ],
}
