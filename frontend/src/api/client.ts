import axios from 'axios'
import type { ApiErrorBody } from './types'

export const TOKEN_STORAGE_KEY = 'mysportsapp.token'

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY)
}

export function setStoredToken(token: string | null): void {
  if (token) {
    localStorage.setItem(TOKEN_STORAGE_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
  }
}

// Same-origin "/api" works when nginx proxies to the backend (docker-compose,
// local dev via the Vite proxy). Deploying frontend and backend as separate
// services with their own origins (e.g. two Cloud Run services) needs an
// absolute URL instead, baked in at build time via the VITE_API_BASE_URL
// build arg in Dockerfile - see docs/deployment.md.
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

export const apiClient = axios.create({
  baseURL,
})

apiClient.interceptors.request.use((config) => {
  const token = getStoredToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      setStoredToken(null)
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

/** Extracts a human-readable message from a failed axios request, falling
 * back to something reasonable when the backend didn't send the expected
 * error shape (network error, timeout, etc). */
export function getApiErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    const body = error.response?.data
    if (body?.message) {
      return body.message
    }
    if (error.message) {
      return error.message
    }
  }
  if (error instanceof Error) {
    return error.message
  }
  return fallback
}
