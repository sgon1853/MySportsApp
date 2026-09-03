import { apiClient } from './client'
import type { ActivityDetail, ActivityFilters, ActivitySummary } from './types'

export async function getActivities(filters: ActivityFilters = {}): Promise<ActivitySummary[]> {
  const params: Record<string, string> = {}
  if (filters.type) params.type = filters.type
  if (filters.from) params.from = filters.from
  if (filters.to) params.to = filters.to

  const { data } = await apiClient.get<ActivitySummary[]>('/v1/activities', { params })
  return data
}

export async function getActivity(id: string): Promise<ActivityDetail> {
  const { data } = await apiClient.get<ActivityDetail>(`/v1/activities/${encodeURIComponent(id)}`)
  return data
}
