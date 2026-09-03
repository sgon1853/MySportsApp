import { apiClient } from './client'
import type { InviteRequest, InviteResponse } from './types'

export async function inviteUser(payload: InviteRequest): Promise<InviteResponse> {
  const { data } = await apiClient.post<InviteResponse>('/v1/admin/invite', payload)
  return data
}
