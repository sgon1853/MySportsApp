import { apiClient } from './client'
import type { ImportBatchResult, ImportProvider } from './types'

export async function getProviders(): Promise<ImportProvider[]> {
  const { data } = await apiClient.get<ImportProvider[]>('/v1/imports/providers')
  return data
}

export async function uploadActivity(file: File, providerId: string): Promise<ImportBatchResult> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('providerId', providerId)

  // A 422 with the ImportBatchResult shape (status FAILED) is a valid,
  // expected response for an unparseable file, not just a thrown error -
  // treat it as success from axios's point of view so callers can inspect
  // `status`/`errors` uniformly instead of catching it separately.
  // Deliberately no explicit Content-Type header: axios/XHR needs to set it
  // itself (including the multipart boundary) when the body is a FormData -
  // overriding it manually would produce a boundary-less header the backend
  // can't parse.
  const { data } = await apiClient.post<ImportBatchResult>('/v1/imports', formData, {
    validateStatus: (status) => (status >= 200 && status < 300) || status === 422,
  })
  return data
}
