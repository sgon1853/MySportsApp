import { useState } from 'react'
import type { FormEvent } from 'react'
import { inviteUser } from '../api/admin'
import { getApiErrorMessage } from '../api/client'
import type { InviteResponse } from '../api/types'

function buildInviteLink(inviteToken: string): string {
  return `${window.location.origin}/accept-invite?inviteToken=${encodeURIComponent(inviteToken)}`
}

export function AdminInvitePage() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<InviteResponse | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [copied, setCopied] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setResult(null)
    setCopied(false)
    setIsSubmitting(true)
    try {
      const response = await inviteUser({ email })
      setResult(response)
      setEmail('')
    } catch (err) {
      setError(getApiErrorMessage(err, 'Could not send the invite.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleCopy() {
    if (!result) return
    const link = buildInviteLink(result.inviteToken)
    try {
      await navigator.clipboard.writeText(link)
      setCopied(true)
    } catch {
      setCopied(false)
    }
  }

  return (
    <div className="page">
      <h1>Invite a user</h1>
      <form className="inline-form" onSubmit={handleSubmit}>
        {error && (
          <div role="alert" className="banner banner--error">
            {error}
          </div>
        )}
        <label htmlFor="invite-email">Email</label>
        <input
          id="invite-email"
          name="email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Sending…' : 'Send invite'}
        </button>
      </form>

      {result && (
        <div className="card invite-result">
          <p>
            Invite created for <strong>{result.email}</strong>, expires{' '}
            {new Date(result.expiresAt).toLocaleString()}.
          </p>
          <div className="invite-result__link">
            <input type="text" readOnly value={buildInviteLink(result.inviteToken)} aria-label="Invite link" />
            <button type="button" onClick={handleCopy}>
              {copied ? 'Copied!' : 'Copy invite link'}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
