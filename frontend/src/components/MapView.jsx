import React from 'react';
import { MapContainer, TileLayer, Polygon, Polyline, CircleMarker, Marker, Popup, useMapEvents } from 'react-leaflet';

function toLeafletLatLng([lng, lat]) {
  return [lat, lng];
}

function ClickCapture({ onMapClick }) {
  useMapEvents({
    click(e) {
      if (onMapClick) {
        onMapClick({ lat: e.latlng.lat, lng: e.latlng.lng });
      }
    },
  });
  return null;
}

/**
 * @param {Array} drawingPoints - puntos [{lat,lng}] acumulados mientras se dibuja un polígono
 */
export default function MapView({
  buildings = [],
  rooms = [],
  center = [-33.4489, -70.6693],
  zoom = 17,
  onMapClick = null,
  pendingMarker = null,
  drawingPoints = [],
}) {
  return (
    <MapContainer center={center} zoom={zoom} style={{ height: '400px', width: '100%', borderRadius: '8px' }}>
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
      />

      {onMapClick && <ClickCapture onMapClick={onMapClick} />}

      {buildings.map((b) => {
        const geom = JSON.parse(b.geomGeoJson);
        if (geom.type !== 'Polygon') return null;
        const positions = geom.coordinates[0].map(toLeafletLatLng);
        return (
          <Polygon key={b.id} positions={positions} pathOptions={{ color: b.color || '#0d6efd', fillOpacity: 0.3 }}>
            <Popup>{b.name}</Popup>
          </Polygon>
        );
      })}

      {rooms.map((r) => {
        const geom = JSON.parse(r.geomGeoJson);
        if (geom.type !== 'Point') return null;
        const position = toLeafletLatLng(geom.coordinates);
        return (
          <Marker key={r.id} position={position}>
            <Popup>{r.popupText || r.name}</Popup>
          </Marker>
        );
      })}

      {pendingMarker && <Marker position={pendingMarker} />}

      {drawingPoints.length > 0 && (
        <>
          <Polyline
            positions={drawingPoints.map((p) => [p.lat, p.lng])}
            pathOptions={{ color: '#dc3545', dashArray: '6 6', weight: 3 }}
          />
          {drawingPoints.map((p, i) => (
            <CircleMarker
              key={i}
              center={[p.lat, p.lng]}
              radius={6}
              pathOptions={{ color: '#dc3545', fillColor: '#dc3545', fillOpacity: 1 }}
            />
          ))}
        </>
      )}
    </MapContainer>
  );
}