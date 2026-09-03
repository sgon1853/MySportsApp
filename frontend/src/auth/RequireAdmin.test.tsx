import { describe, expect, it } from 'vitest'
import { screen } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, signInAs } from '../test/testUtils'
import { mockAdmin, mockUser } from '../test/mocks/fixtures'
import { RequireAdmin } from './RequireAdmin'

function AdminPage() {
  return <p>Admin content</p>
}

function LoginStub() {
  return <p>Login page</p>
}

function ActivitiesStub() {
  return <p>Activities page</p>
}

function TestRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginStub />} />
      <Route path="/activities" element={<ActivitiesStub />} />
      <Route
        path="/admin/invite"
        element={
          <RequireAdmin>
            <AdminPage />
          </RequireAdmin>
        }
      />
    </Routes>
  )
}

describe('RequireAdmin', () => {
  it('redirects to /login when there is no authenticated user', () => {
    renderWithProviders(<TestRoutes />, { route: '/admin/invite' })

    expect(screen.getByText('Login page')).toBeInTheDocument()
  })

  it('redirects non-admin users to /activities', () => {
    signInAs(mockUser)
    renderWithProviders(<TestRoutes />, { route: '/admin/invite' })

    expect(screen.getByText('Activities page')).toBeInTheDocument()
    expect(screen.queryByText('Admin content')).not.toBeInTheDocument()
  })

  it('renders the admin content for admin users', () => {
    signInAs(mockAdmin)
    renderWithProviders(<TestRoutes />, { route: '/admin/invite' })

    expect(screen.getByText('Admin content')).toBeInTheDocument()
  })
})
