import { useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getProviders, uploadActivity } from '../../api/imports'
import { getApiErrorMessage } from '../../api/client'
import type { ImportBatchResult, ImportBatchStatus } from '../../api/types'
import { ProviderSelect } from './ProviderSelect'

/** One file's outcome within a (possibly multi-file) upload batch. `result`
 * is null when the request itself failed (network error, unexpected 5xx) -
 * as opposed to a handled 422, which still produces a normal `result` with
 * status FAILED. Either way the file is reported individually rather than
 * aborting the rest of the batch. */
interface FileUploadOutcome {
  fileName: string
  result: ImportBatchResult | null
  error: string | null
}

export function UploadPage() {
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const providersQuery = useQuery({
    queryKey: ['providers'],
    queryFn: getProviders,
  })

  const [providerId, setProviderId] = useState('')
  const [selectedFiles, setSelectedFiles] = useState<File[]>([])
  const [formError, setFormError] = useState<string | null>(null)

  const uploadMutation = useMutation({
    mutationFn: async ({ files, providerId }: { files: File[]; providerId: string }) => {
      const outcomes: FileUploadOutcome[] = []
      // Sequential, not parallel: if the same activity is present in two of
      // the selected files, sequential uploads let the second correctly see
      // the first as already-inserted via the dedup check, instead of both
      // requests racing past that check before either is persisted.
      for (const file of files) {
        try {
          const result = await uploadActivity(file, providerId)
          outcomes.push({ fileName: file.name, result, error: null })
        } catch (error) {
          outcomes.push({ fileName: file.name, result: null, error: getApiErrorMessage(error) })
        }
      }
      return outcomes
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['activities'] })
    },
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError(null)
    uploadMutation.reset()

    if (selectedFiles.length === 0) {
      setFormError('Choose at least one file to upload.')
      return
    }
    if (!providerId) {
      setFormError('Select a provider.')
      return
    }

    uploadMutation.mutate({ files: selectedFiles, providerId })
  }

  function handleReset() {
    setSelectedFiles([])
    setProviderId('')
    setFormError(null)
    uploadMutation.reset()
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const outcomes = uploadMutation.data

  return (
    <div className="page">
      <h1>Upload activities</h1>

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

          <label htmlFor="provider-select">Device / provider</label>
          <ProviderSelect
            providers={providersQuery.data}
            value={providerId}
            onChange={setProviderId}
            disabled={uploadMutation.isPending}
          />

          <label htmlFor="activity-file">Activity file(s)</label>
          <input
            id="activity-file"
            name="file"
            type="file"
            multiple
            ref={fileInputRef}
            disabled={uploadMutation.isPending}
            onChange={(e) => setSelectedFiles(Array.from(e.target.files ?? []))}
          />
          {selectedFiles.length > 1 && (
            <p className="upload-form__file-count">{selectedFiles.length} files selected</p>
          )}

          <div className="upload-form__actions">
            <button type="submit" disabled={uploadMutation.isPending}>
              {uploadMutation.isPending
                ? 'Uploading…'
                : selectedFiles.length > 1
                  ? `Upload ${selectedFiles.length} files`
                  : 'Upload'}
            </button>
            <button type="button" onClick={handleReset} disabled={uploadMutation.isPending}>
              Reset
            </button>
          </div>
        </form>
      )}

      {outcomes && <UploadResultSummary outcomes={outcomes} />}
    </div>
  )
}

function overallStatusOf(outcomes: FileUploadOutcome[]): ImportBatchStatus {
  const statuses = outcomes.map((o) => o.result?.status ?? 'FAILED')
  if (statuses.every((s) => s === 'SUCCESS')) return 'SUCCESS'
  if (statuses.every((s) => s === 'FAILED')) return 'FAILED'
  return 'PARTIAL'
}

function UploadResultSummary({ outcomes }: { outcomes: FileUploadOutcome[] }) {
  const multiple = outcomes.length > 1
  const overallStatus = overallStatusOf(outcomes)

  const totals = outcomes.reduce(
    (acc, o) => ({
      recordsParsed: acc.recordsParsed + (o.result?.recordsParsed ?? 0),
      recordsInserted: acc.recordsInserted + (o.result?.recordsInserted ?? 0),
      recordsDeduped: acc.recordsDeduped + (o.result?.recordsDeduped ?? 0),
      recordsFailed: acc.recordsFailed + (o.result?.recordsFailed ?? 0),
    }),
    { recordsParsed: 0, recordsInserted: 0, recordsDeduped: 0, recordsFailed: 0 },
  )

  // Single-file uploads keep exactly the old, unprefixed error text; a
  // multi-file batch prefixes each error with the file it came from so a
  // failure in one file doesn't get lost among the others.
  const allErrors = outcomes.flatMap((o) => {
    const prefix = multiple ? `${o.fileName}: ` : ''
    if (o.error) return [`${prefix}${o.error}`]
    return (o.result?.errors ?? []).map((err) => `${prefix}${err}`)
  })

  return (
    <section className="card upload-result" aria-label="Upload result" data-status={overallStatus}>
      <h2>
        Import{' '}
        {overallStatus === 'SUCCESS' ? 'succeeded' : overallStatus === 'PARTIAL' ? 'partially succeeded' : 'failed'}
      </h2>
      <dl className="stat-grid">
        <div>
          <dt>Parsed</dt>
          <dd>{totals.recordsParsed}</dd>
        </div>
        <div>
          <dt>Inserted</dt>
          <dd>{totals.recordsInserted}</dd>
        </div>
        <div>
          <dt>Deduped</dt>
          <dd>{totals.recordsDeduped}</dd>
        </div>
        <div>
          <dt>Failed</dt>
          <dd>{totals.recordsFailed}</dd>
        </div>
      </dl>
      {multiple && (
        <ul className="upload-result__files" aria-label="Per-file results">
          {outcomes.map((o) => (
            <li key={o.fileName}>
              <strong>{o.fileName}</strong>:{' '}
              {o.error
                ? `failed to upload (${o.error})`
                : `${o.result?.status.toLowerCase()} — ${o.result?.recordsInserted} inserted, ${o.result?.recordsDeduped} deduped`}
            </li>
          ))}
        </ul>
      )}
      {allErrors.length > 0 && (
        <div>
          <p>Errors:</p>
          <ul>
            {allErrors.map((err, i) => (
              <li key={`${i}-${err}`}>{err}</li>
            ))}
          </ul>
        </div>
      )}
    </section>
  )
}
