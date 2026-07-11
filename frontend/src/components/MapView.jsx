import React from 'react';
import { MapContainer, TileLayer, Polygon, Marker, Popup } from 'react-leaflet';

/**
 * Convierte coordenadas GeoJSON [lng, lat] a formato Leaflet [lat, lng].
 * GeoJSON siempre guarda longitud primero; Leaflet espera latitud primero.
 * Esta inversión es la causa más común de "el mapa se ve en el mar equivocado".
 */
function toLeafletLatLng([lng, lat]) {
  return [lat, lng];
}

/**
 * @param {Array} buildings - lista de edificios, cada uno con { id, name, geomGeoJson, color? }
 * @param {Array} rooms - lista opcional de salas, cada una con { id, name, geomGeoJson, popupText? }
 * @param {Array} center - [lat, lng] inicial del mapa
 * @param {number} zoom - nivel de zoom inicial
 */
export default function MapView({ buildings = [], rooms = [], center = [-33.4489, -70.6693], zoom = 17 }) {
  return (
    <MapContainer center={center} zoom={zoom} style={{ height: '400px', width: '100%', borderRadius: '8px' }}>
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
      />

      {buildings.map((b) => {
        const geom = JSON.parse(b.geomGeoJson);
        if (geom.type !== 'Polygon') return null;
        const positions = geom.coordinates[0].map(toLeafletLatLng);

        return (
          <Polygon
            key={b.id}
            positions={positions}
            pathOptions={{ color: b.color || '#0d6efd', fillOpacity: 0.3 }}
          >
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
    </MapContainer>
  );
}