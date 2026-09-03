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
})
