import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { ChartPoint } from './chartData'

export function ElevationChart({ data }: { data: ChartPoint[] }) {
  const hasElevation = data.some((point) => point.elevationMeters !== null)

  if (!hasElevation) {
    return <p className="chart-empty">No elevation data recorded for this activity.</p>
  }

  return (
    <ResponsiveContainer width="100%" height={220}>
      <AreaChart data={data} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="distanceKm" tickFormatter={(v: number) => `${v.toFixed(1)} km`} minTickGap={40} />
        <YAxis width={48} unit=" m" domain={['dataMin - 5', 'dataMax + 5']} />
        <Tooltip
          labelFormatter={(value) => `${Number(value).toFixed(2)} km`}
          formatter={(value) => [`${Math.round(Number(value))} m`, 'Elevation']}
        />
        <Area
          type="monotone"
          dataKey="elevationMeters"
          stroke="#2f6fe0"
          fill="#2f6fe033"
          isAnimationActive={false}
        />
      </AreaChart>
    </ResponsiveContainer>
  )
}
