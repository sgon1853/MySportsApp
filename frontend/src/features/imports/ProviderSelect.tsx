import type { ImportProvider } from '../../api/types'

interface ProviderSelectProps {
  providers: ImportProvider[]
  value: string
  onChange: (providerId: string) => void
  disabled?: boolean
}

export function ProviderSelect({ providers, value, onChange, disabled }: ProviderSelectProps) {
  return (
    <select
      id="provider-select"
      name="providerId"
      value={value}
      disabled={disabled}
      required
      onChange={(e) => onChange(e.target.value)}
    >
      <option value="" disabled>
        Select a provider…
      </option>
      {providers.map((provider) => (
        <option key={provider.providerId} value={provider.providerId}>
          {provider.displayName}
        </option>
      ))}
    </select>
  )
}
