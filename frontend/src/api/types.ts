// Shared types matching the backend API contract exactly.

export type UserRole = 'ADMIN' | 'USER'

export interface User {
  id: string
  email: string
  role: UserRole
}

export interface LoginRequest {
  email: string
  password: string
}

export interface AcceptInviteRequest {
  inviteToken: string
  password: string
}

export interface AuthResponse {
  token: string
  user: User
}

export interface InviteRequest {
  email: string
}

export interface InviteResponse {
  email: string
  inviteToken: string
  expiresAt: string
}

export interface ImportProvider {
  providerId: string
  displayName: string
  supportedExtensions: string[]
}

export type ImportBatchStatus = 'SUCCESS' | 'PARTIAL' | 'FAILED'

export interface ImportBatchResult {
  batchId: string
  providerId: string
  status: ImportBatchStatus
  recordsParsed: number
  recordsInserted: number
  recordsDeduped: number
  recordsFailed: number
  errors: string[]
}

/** Currently only GPS_TRACK exists; the registry is keyed by this string so
 * new visualization types can be added without touching existing code. */
export type VisualizationType = 'GPS_TRACK' | (string & {})

export interface ActivitySummary {
  id: string
  activityType: string
  visualizationType: VisualizationType
  startTime: string
  durationSeconds: number
  distanceMeters: number | null
  avgHr: number | null
  maxHr: number | null
  calories: number | null
  elevationGainMeters: number | null
  sourceProviderId: string
}

export interface TrackPoint {
  timestamp: string
  lat: number | null
  lon: number | null
  elevationMeters: number | null
  heartRate: number | null
}

export interface ActivityDetail extends ActivitySummary {
  trackPoints: TrackPoint[]
}

export interface ActivityFilters {
  type?: string
  from?: string
  to?: string
}

export interface ApiErrorBody {
  message: string
  details?: string[]
}
