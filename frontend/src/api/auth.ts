import { apiClient } from './client'
import type { AcceptInviteRequest, AuthResponse, LoginRequest } from './types'

export async function login(payload: LoginRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/v1/auth/login', payload)
  return data
}

export async function acceptInvite(payload: AcceptInviteRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/v1/auth/accept-invite', payload)
  return data
}
