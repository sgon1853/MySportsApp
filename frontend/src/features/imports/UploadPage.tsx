import { useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getProviders, uploadActivity } from '../../api/imports'
import { getApiErrorMessage } from '../../api/client'
import type { ImportBatchResult } from '../../api/types'
import { ProviderSelect } from './ProviderSelect'

export function UploadPage() {
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const providersQuery = useQuery({
    queryKey: ['providers'],
    queryFn: getProviders,
  })

  const [providerId, setProviderId] = useState('')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  const uploadMutation = useMutation({
    mutationFn: ({ file, providerId }: { file: File; providerId: string }) => uploadActivity(file, providerId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['activities'] })
    },
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError(null)
    uploadMutation.reset()

    if (!selectedFile) {
      setFormError('Choose a file to upload.')
      return
    }
    if (!providerId) {
      setFormError('Select a provider.')
      return
    }

    uploadMutation.mutate({ file: selectedFile, providerId })
  }

  function handleReset() {
    setSelectedFile(null)
    setProviderId('')
    setFormError(null)
    uploadMutation.reset()
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const result: ImportBatchResult | undefined = uploadMutation.data

  return (
    <div className="page">
      <h1>Upload an activity</h1>

      {providersQuery.isLoading && <p>Loading providers…</p>}
      {providersQuery.isError && (
        <div role="alert" className="banner banner--error">
          {getApiErrorMessage(providersQuery.error, 'Could not load providers.')}
        </div>
      )}

      {providersQuery.data && (
        <form className="upload-form" onSubmit={handleSubmit}>
          {formError && (
            <div role="alert" className="banner banner--error">
              {formError}
            </div>
          )}
          {uploadMutation.isError && (
            <div role="alert" className="banner banner--error">
              {getApiErrorMessage(uploadMutation.error, 'Upload failed.')}
            </div>
          )}

          <label htmlFor="provider-select">Device / provider</label>
          <ProviderSelect
            providers={providersQuery.data}
            value={providerId}
            onChange={setProviderId}
            disabled={uploadMutation.isPending}
          />

          <label htmlFor="activity-file">Activity file</label>
          <input
            id="activity-file"
            name="file"
            type="file"
            ref={fileInputRef}
            disabled={uploadMutation.isPending}
            onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)}
          />

          <div className="upload-form__actions">
            <button type="submit" disabled={uploadMutation.isPending}>
              {uploadMutation.isPending ? 'Uploading…' : 'Upload'}
            </button>
            <button type="button" onClick={handleReset} disabled={uploadMutation.isPending}>
              Reset
            </button>
          </div>
        </form>
      )}

      {result && <UploadResultSummary result={result} />}
    </div>
  )
}

function UploadResultSummary({ result }: { result: ImportBatchResult }) {
  return (
    <section className="card upload-result" aria-label="Upload result" data-status={result.status}>
      <h2>
        Import {result.status === 'SUCCESS' ? 'succeeded' : result.status === 'PARTIAL' ? 'partially succeeded' : 'failed'}
      </h2>
      <dl className="stat-grid">
        <div>
          <dt>Parsed</dt>
          <dd>{result.recordsParsed}</dd>
        </div>
        <div>
          <dt>Inserted</dt>
          <dd>{result.recordsInserted}</dd>
        </div>
        <div>
          <dt>Deduped</dt>
          <dd>{result.recordsDeduped}</dd>
        </div>
        <div>
          <dt>Failed</dt>
          <dd>{result.recordsFailed}</dd>
        </div>
      </dl>
      {result.errors.length > 0 && (
        <div>
          <p>Errors:</p>
          <ul>
            {result.errors.map((err) => (
              <li key={err}>{err}</li>
            ))}
          </ul>
        </div>
      )}
    </section>
  )
}
