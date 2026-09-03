import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { getApiErrorMessage } from '../api/client'

export function AcceptInvitePage() {
  const { acceptInvite } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const inviteToken = searchParams.get('inviteToken') ?? ''

  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)

    if (!inviteToken) {
      setError('This invite link is missing its token. Ask an admin to resend the invite.')
      return
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    setIsSubmitting(true)
    try {
      await acceptInvite({ inviteToken, password })
      navigate('/activities', { replace: true })
    } catch (err) {
      setError(getApiErrorMessage(err, 'Could not accept the invite. The link may have expired.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit}>
        <h1>Accept invite</h1>
        {error && (
          <div role="alert" className="banner banner--error">
            {error}
          </div>
        )}
        {!inviteToken && (
          <div role="alert" className="banner banner--error">
            No invite token found in this link.
          </div>
        )}
        <label htmlFor="invite-password">Password</label>
        <input
          id="invite-password"
          name="password"
          type="password"
          autoComplete="new-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <label htmlFor="invite-confirm-password">Confirm password</label>
        <input
          id="invite-confirm-password"
          name="confirmPassword"
          type="password"
          autoComplete="new-password"
          required
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
        />
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Setting up your account…' : 'Set password & log in'}
        </button>
      </form>
    </div>
  )
}
