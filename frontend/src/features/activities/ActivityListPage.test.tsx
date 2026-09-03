import { describe, expect, it } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders, signInAs } from '../../test/testUtils'
import { mockUser } from '../../test/mocks/fixtures'
import { ActivityListPage } from './ActivityListPage'

describe('ActivityListPage', () => {
  it('renders a row for each activity returned by the API', async () => {
    signInAs(mockUser)
    renderWithProviders(<ActivityListPage />)

    expect(await screen.findByRole('link', { name: 'Run' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ride' })).toBeInTheDocument()

    const rows = screen.getAllByRole('row')
    // header row + 2 activity rows
    expect(rows).toHaveLength(3)
  })

  it('links each row to its activity detail page', async () => {
    signInAs(mockUser)
    renderWithProviders(<ActivityListPage />)

    const runLink = await screen.findByRole('link', { name: 'Run' })
    expect(runLink).toHaveAttribute('href', '/activities/act-1')
  })
})
