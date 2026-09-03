import { useEffect } from 'react'
import { MapContainer, Polyline, TileLayer, useMap } from 'react-leaflet'
import type { LatLngExpression, LatLngBoundsExpression } from 'leaflet'

function FitBounds({ bounds }: { bounds: LatLngBoundsExpression }) {
  const map = useMap()
  useEffect(() => {
    map.fitBounds(bounds, { padding: [24, 24] })
  }, [map, bounds])
  return null
}

export function GpsTrackMap({ positions }: { positions: LatLngExpression[] }) {
  if (positions.length === 0) {
    return <p className="chart-empty">No GPS coordinates recorded for this activity.</p>
  }

  return (
    <MapContainer
      center={positions[0]}
      zoom={13}
      scrollWheelZoom={false}
      style={{ height: 320, width: '100%' }}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <Polyline positions={positions} pathOptions={{ color: '#e0522f', weight: 4 }} />
      <FitBounds bounds={positions as LatLngBoundsExpression} />
    </MapContainer>
  )
}
