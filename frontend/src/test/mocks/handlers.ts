import { http, HttpResponse } from 'msw'
import type { ImportBatchResult } from '../../api/types'
import { mockActivities, mockActivityDetail, mockAdmin, mockProviders, mockToken, mockUser } from './fixtures'

const successResult: ImportBatchResult = {
  batchId: 'batch-1',
  providerId: 'suunto-gpx',
  status: 'SUCCESS',
  recordsParsed: 1,
  recordsInserted: 1,
  recordsDeduped: 0,
  recordsFailed: 0,
  errors: [],
}

export const handlers = [
  http.post('/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string }
    if (body.email === mockAdmin.email && body.password === 'password') {
      return HttpResponse.json({ token: mockToken, user: mockAdmin })
    }
    if (body.email === mockUser.email && body.password === 'password') {
      return HttpResponse.json({ token: mockToken, user: mockUser })
    }
    return HttpResponse.json({ message: 'Invalid email or password' }, { status: 401 })
  }),

  http.post('/api/v1/auth/accept-invite', async () => {
    return HttpResponse.json({ token: mockToken, user: mockUser })
  }),

  http.post('/api/v1/admin/invite', async ({ request }) => {
    const body = (await request.json()) as { email: string }
    return HttpResponse.json(
      { email: body.email, inviteToken: 'invite-token-abc', expiresAt: '2026-09-10T00:00:00Z' },
      { status: 201 },
    )
  }),

  http.get('/api/v1/imports/providers', () => {
    return HttpResponse.json(mockProviders)
  }),

  http.post('/api/v1/imports', () => {
    return HttpResponse.json(successResult)
  }),

  http.get('/api/v1/activities', () => {
    return HttpResponse.json(mockActivities)
  }),

  http.get('/api/v1/activities/:id', ({ params }) => {
    if (params.id !== mockActivityDetail.id) {
      return HttpResponse.json({ message: 'Activity not found' }, { status: 404 })
    }
    return HttpResponse.json(mockActivityDetail)
  }),
]
