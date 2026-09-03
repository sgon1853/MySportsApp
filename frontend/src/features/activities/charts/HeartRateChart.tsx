import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { ChartPoint } from './chartData'
import { formatElapsed } from './chartData'

export function HeartRateChart({ data }: { data: ChartPoint[] }) {
  const hasHeartRate = data.some((point) => point.heartRate !== null)

  if (!hasHeartRate) {
    return <p className="chart-empty">No heart rate data recorded for this activity.</p>
  }

  return (
    <ResponsiveContainer width="100%" height={220}>
      <LineChart data={data} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="elapsedSeconds" tickFormatter={formatElapsed} minTickGap={40} />
        <YAxis width={40} domain={['dataMin - 5', 'dataMax + 5']} unit=" bpm" />
        <Tooltip
          labelFormatter={(value) => `Elapsed ${formatElapsed(Number(value))}`}
          formatter={(value) => [`${value} bpm`, 'Heart rate']}
        />
        <Line type="monotone" dataKey="heartRate" stroke="#e0522f" dot={false} isAnimationActive={false} />
      </LineChart>
    </ResponsiveContainer>
  )
}
