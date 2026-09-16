import { describe, expect, it } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { renderWithProviders, signInAs } from '../../test/testUtils'
import { server } from '../../test/mocks/server'
import { mockUser } from '../../test/mocks/fixtures'
import { UploadPage } from './UploadPage'

function selectFile(input: HTMLElement) {
  const file = new File(['gpx-content'], 'activity.gpx', { type: 'application/gpx+xml' })
  return userEvent.upload(input, file)
}

function makeFile(name: string) {
  return new File(['gpx-content'], name, { type: 'application/gpx+xml' })
}

describe('UploadPage', () => {
  it('renders providers fetched from the API', async () => {
    signInAs(mockUser)
    renderWithProviders(<UploadPage />)

    expect(await screen.findByRole('option', { name: 'Suunto (GPX)' })).toBeInTheDocument()
  })

  it('uploads a file and shows the success result summary', async () => {
    signInAs(mockUser)
    const user = userEvent.setup()
    renderWithProviders(<UploadPage />)

    await screen.findByRole('option', { name: 'Suunto (GPX)' })
    await user.selectOptions(screen.getByLabelText(/device \/ provider/i), 'suunto-gpx')
    await selectFile(screen.getByLabelText(/activity file/i))
    await user.click(screen.getByRole('button', { name: /^upload$/i }))

    expect(await screen.findByText(/import succeeded/i)).toBeInTheDocument()
    const stats = screen.getAllByRole('definition')
    expect(stats.map((el) => el.textContent)).toEqual(['1', '1', '0', '0']) // parsed, inserted, deduped, failed
  })

  it('shows a deduped/failed result when the backend reports a partial import', async () => {
    server.use(
      http.post('/api/v1/imports', () =>
        HttpResponse.json({
          batchId: 'batch-2',
          providerId: 'suunto-gpx',
          status: 'PARTIAL',
          recordsParsed: 10,
          recordsInserted: 6,
          recordsDeduped: 3,
          recordsFailed: 1,
          errors: ['Row 8: missing timestamp'],
        }),
      ),
    )
    signInAs(mockUser)
    const user = userEvent.setup()
    renderWithProviders(<UploadPage />)

    await screen.findByRole('option', { name: 'Suunto (GPX)' })
    await user.selectOptions(screen.getByLabelText(/device \/ provider/i), 'suunto-gpx')
    await selectFile(screen.getByLabelText(/activity file/i))
    await user.click(screen.getByRole('button', { name: /^upload$/i }))

    expect(await screen.findByText(/import partially succeeded/i)).toBeInTheDocument()
    expect(screen.getByText('Row 8: missing timestamp')).toBeInTheDocument()

    const stats = screen.getAllByRole('definition')
    expect(stats.map((el) => el.textContent)).toEqual(['10', '6', '3', '1']) // parsed, inserted, deduped, failed
  })

  it('uploads multiple files sequentially and shows aggregated + per-file results', async () => {
    let callCount = 0
    server.use(
      http.post('/api/v1/imports', () => {
        callCount += 1
        // First file: a fresh insert. Second file: reported as a duplicate.
        // Sequential (not parallel) uploads are what makes this distinction
        // meaningful to test at all.
        return callCount === 1
          ? HttpResponse.json({
              batchId: 'batch-run1',
              providerId: 'suunto-gpx',
              status: 'SUCCESS',
              recordsParsed: 1,
              recordsInserted: 1,
              recordsDeduped: 0,
              recordsFailed: 0,
              errors: [],
            })
          : HttpResponse.json({
              batchId: 'batch-run2',
              providerId: 'suunto-gpx',
              status: 'SUCCESS',
              recordsParsed: 1,
              recordsInserted: 0,
              recordsDeduped: 1,
              recordsFailed: 0,
              errors: [],
            })
      }),
    )

    signInAs(mockUser)
    const user = userEvent.setup()
    renderWithProviders(<UploadPage />)

    await screen.findByRole('option', { name: 'Suunto (GPX)' })
    await user.selectOptions(screen.getByLabelText(/device \/ provider/i), 'suunto-gpx')
    await userEvent.upload(screen.getByLabelText(/activity file/i), [makeFile('run1.gpx'), makeFile('run2.gpx')])
    expect(screen.getByText('2 files selected')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /upload 2 files/i }))

    expect(await screen.findByText(/import succeeded/i)).toBeInTheDocument()
    const stats = screen.getAllByRole('definition')
    expect(stats.map((el) => el.textContent)).toEqual(['2', '1', '1', '0']) // aggregated parsed, inserted, deduped, failed

    const fileResults = screen.getByRole('list', { name: 'Per-file results' })
    expect(fileResults).toHaveTextContent('run1.gpx')
    expect(fileResults).toHaveTextContent('run2.gpx')
    expect(callCount).toBe(2)
  })

  it('reports a per-file failure without losing the rest of the batch', async () => {
    server.use(
      http.post('/api/v1/imports', () => HttpResponse.json({ message: 'boom' }, { status: 500 })),
    )

    signInAs(mockUser)
    const user = userEvent.setup()
    renderWithProviders(<UploadPage />)

    await screen.findByRole('option', { name: 'Suunto (GPX)' })
    await user.selectOptions(screen.getByLabelText(/device \/ provider/i), 'suunto-gpx')
    await userEvent.upload(screen.getByLabelText(/activity file/i), [makeFile('bad.gpx'), makeFile('also-bad.gpx')])
    await user.click(screen.getByRole('button', { name: /upload 2 files/i }))

    expect(await screen.findByText(/import failed/i)).toBeInTheDocument()
    // Exact matches, not regexes: "bad.gpx: boom" is a substring of
    // "also-bad.gpx: boom", so a loose pattern would match both of these.
    expect(screen.getByText('bad.gpx: boom')).toBeInTheDocument()
    expect(screen.getByText('also-bad.gpx: boom')).toBeInTheDocument()
  })
})
