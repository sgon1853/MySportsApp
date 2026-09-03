import type { ReactNode } from 'react'
import { NavLink } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function Layout({ children }: { children: ReactNode }) {
  const { user, logout, isAuthenticated } = useAuth()

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header__brand">MySportsApp</div>
        {isAuthenticated && (
          <nav className="app-header__nav" aria-label="Main navigation">
            <NavLink to="/activities" className={({ isActive }) => (isActive ? 'active' : '')}>
              Activities
            </NavLink>
            <NavLink to="/upload" className={({ isActive }) => (isActive ? 'active' : '')}>
              Upload
            </NavLink>
            {user?.role === 'ADMIN' && (
              <NavLink to="/admin/invite" className={({ isActive }) => (isActive ? 'active' : '')}>
                Admin
              </NavLink>
            )}
          </nav>
        )}
        {isAuthenticated && (
          <div className="app-header__user">
            <span className="app-header__email">{user?.email}</span>
            <button type="button" onClick={logout} className="link-button">
              Logout
            </button>
          </div>
        )}
      </header>
      <main className="app-main">{children}</main>
    </div>
  )
}
