import { describe, expect, it } from 'vitest'
import { screen } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, signInAs } from '../test/testUtils'
import { mockUser } from '../test/mocks/fixtures'
import { RequireAuth } from './RequireAuth'

function ProtectedPage() {
  return <p>Protected content</p>
}

function LoginStub() {
  return <p>Login page</p>
}

function TestRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginStub />} />
      <Route
        path="/protected"
        element={
          <RequireAuth>
            <ProtectedPage />
          </RequireAuth>
        }
      />
    </Routes>
  )
}

describe('RequireAuth', () => {
  it('redirects to /login when there is no authenticated user', () => {
    renderWithProviders(<TestRoutes />, { route: '/protected' })

    expect(screen.getByText('Login page')).toBeInTheDocument()
    expect(screen.queryByText('Protected content')).not.toBeInTheDocument()
  })

  it('renders the protected content when the user is authenticated', () => {
    signInAs(mockUser)
    renderWithProviders(<TestRoutes />, { route: '/protected' })

    expect(screen.getByText('Protected content')).toBeInTheDocument()
  })
})
