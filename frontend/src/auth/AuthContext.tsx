import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import * as authApi from '../api/auth'
import { getStoredToken, setStoredToken } from '../api/client'
import type { AcceptInviteRequest, LoginRequest, User } from '../api/types'

const USER_STORAGE_KEY = 'mysportsapp.user'

function readStoredUser(): User | null {
  const raw = localStorage.getItem(USER_STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as User
  } catch {
    return null
  }
}

function storeUser(user: User | null): void {
  if (user) {
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_STORAGE_KEY)
  }
}

interface AuthContextValue {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  login: (payload: LoginRequest) => Promise<void>
  acceptInvite: (payload: AcceptInviteRequest) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getStoredToken())
  const [user, setUser] = useState<User | null>(() => readStoredUser())

  const applySession = useCallback((nextToken: string, nextUser: User) => {
    setStoredToken(nextToken)
    storeUser(nextUser)
    setToken(nextToken)
    setUser(nextUser)
  }, [])

  const login = useCallback(
    async (payload: LoginRequest) => {
      const response = await authApi.login(payload)
      applySession(response.token, response.user)
    },
    [applySession],
  )

  const acceptInvite = useCallback(
    async (payload: AcceptInviteRequest) => {
      const response = await authApi.acceptInvite(payload)
      applySession(response.token, response.user)
    },
    [applySession],
  )

  const logout = useCallback(() => {
    setStoredToken(null)
    storeUser(null)
    setToken(null)
    setUser(null)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(token && user),
      login,
      acceptInvite,
      logout,
    }),
    [user, token, login, acceptInvite, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return ctx
}
