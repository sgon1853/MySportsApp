import type { ReactElement, ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../auth/AuthContext'
import { TOKEN_STORAGE_KEY } from '../api/client'
import type { User } from '../api/types'

export function makeQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
}

/** Seeds localStorage with a logged-in session before AuthProvider mounts,
 * so pages that call useAuth() behave as if the user already logged in. */
export function signInAs(user: User, token = 'test-token'): void {
  localStorage.setItem(TOKEN_STORAGE_KEY, token)
  localStorage.setItem('mysportsapp.user', JSON.stringify(user))
}

interface RenderOptions {
  route?: string
  queryClient?: QueryClient
}

export function renderWithProviders(ui: ReactElement, { route = '/', queryClient }: RenderOptions = {}) {
  const client = queryClient ?? makeQueryClient()

  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={[route]}>
          <AuthProvider>{children}</AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>
    )
  }

  return render(ui, { wrapper: Wrapper })
}
