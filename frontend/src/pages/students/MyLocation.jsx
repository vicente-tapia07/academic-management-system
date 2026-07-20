import React, { useState } from 'react';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import MapView from '../../components/MapView';
import useGeolocation from '../../hooks/useGeolocation';

export default function MyLocation() {
  const { user } = useAuth();
  const { position, error: geoError, loading: geoLoading, requestLocation } = useGeolocation();

  const [room, setRoom] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const [searching, setSearching] = useState(false);
  const [apiError, setApiError] = useState('');

  const handleSearch = async () => {
    if (!position) {
      requestLocation();
      return;
    }
    setSearching(true);
    setApiError('');
    setNotFound(false);
    setRoom(null);
    try {
      const res = await api.post('/api/location/nearest-room', {
        studentId: user.id,
        lat: position.lat,
        lng: position.lng,
      });
      setRoom(res.data);
    } catch (err) {
      if (err.response?.status === 404) {
        setNotFound(true);
      } else {
        setApiError('Error al buscar tu sala más cercana.');
      }
    } finally {
      setSearching(false);
    }
  };

  return (
    <div className="container py-4">
      <h2 className="fw-bold mb-1">Mi Ubicación</h2>
      <p className="text-muted">Encuentra la sala más cercana donde tienes clase activa ahora mismo.</p>

      <div className="d-flex gap-2 mb-4">
        {!position && (
          <button className="btn btn-primary" onClick={requestLocation} disabled={geoLoading}>
            {geoLoading ? 'Obteniendo ubicación...' : '📍 Compartir mi ubicación'}
          </button>
        )}
        {position && (
          <button className="btn btn-primary" onClick={handleSearch} disabled={searching}>
            {searching ? 'Buscando...' : 'Buscar mi sala más cercana'}
          </button>
        )}
      </div>

      {geoError && <div className="alert alert-warning">{geoError}</div>}
      {apiError && <div className="alert alert-danger">{apiError}</div>}
      {notFound && (
        <div className="alert alert-info">
          No tienes clases activas en este momento (según tu horario), así que no hay una sala que sugerirte ahora mismo.
        </div>
      )}

      {room && (
        <div className="card shadow-sm border-0 mb-4">
          <div className="card-body">
            <h5 className="fw-bold mb-1">
              Sala {room.roomCode} — {room.roomName}
            </h5>
            <p className="text-muted mb-1">Asignatura: {room.subjectName}</p>
            <p className="mb-0">
              Distancia: <strong>{Math.round(room.distanceMeters)} m</strong>
            </p>
          </div>
        </div>
      )}

      {position && (
        <MapView
          center={[position.lat, position.lng]}
          zoom={18}
          pendingMarker={[position.lat, position.lng]}
          rooms={room && room.geomGeoJson ? [{ id: room.roomId, name: room.roomName, geomGeoJson: room.geomGeoJson, popupText: `${room.roomCode} — ${room.roomName}` }] : []}
        />
      )}
    </div>
  );
}