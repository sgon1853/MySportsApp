import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAdmin } from './auth/RequireAdmin'
import { RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { ActivityDetailPage } from './features/activities/ActivityDetailPage'
import { ActivityListPage } from './features/activities/ActivityListPage'
import { UploadPage } from './features/imports/UploadPage'
import { AcceptInvitePage } from './pages/AcceptInvitePage'
import { AdminInvitePage } from './pages/AdminInvitePage'
import { LoginPage } from './pages/LoginPage'

export function AppRouter() {
  return (
    <Layout>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/accept-invite" element={<AcceptInvitePage />} />
        <Route path="/" element={<Navigate to="/activities" replace />} />
        <Route
          path="/activities"
          element={
            <RequireAuth>
              <ActivityListPage />
            </RequireAuth>
          }
        />
        <Route
          path="/activities/:id"
          element={
            <RequireAuth>
              <ActivityDetailPage />
            </RequireAuth>
          }
        />
        <Route
          path="/upload"
          element={
            <RequireAuth>
              <UploadPage />
            </RequireAuth>
          }
        />
        <Route
          path="/admin/invite"
          element={
            <RequireAdmin>
              <AdminInvitePage />
            </RequireAdmin>
          }
        />
        <Route path="*" element={<Navigate to="/activities" replace />} />
      </Routes>
    </Layout>
  )
}
