import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../../services/api';
import MapView from '../../components/MapView';

export default function RoomForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);

  const [form, setForm] = useState({
    buildingId: '', code: '', name: '', capacity: '',
  });
  const [selectedPoint, setSelectedPoint] = useState(null); // {lat, lng}
  const [buildings, setBuildings] = useState([]);
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState('');

  useEffect(() => {
    api.get('/api/buildings').then((r) => setBuildings(r.data));
    if (!isEdit) return;
    api.get(`/api/rooms/${id}`).then((r) => {
      const { buildingId, code, name, capacity, geomGeoJson } = r.data;
      setForm({ buildingId, code, name, capacity });
      // Si estamos editando, precargamos el punto ya guardado en el mapa
      const geom = JSON.parse(geomGeoJson);
      setSelectedPoint({ lat: geom.coordinates[1], lng: geom.coordinates[0] });
    });
  }, [id, isEdit]);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleMapClick = ({ lat, lng }) => {
    setSelectedPoint({ lat, lng });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!selectedPoint) {
      setError('Haz clic en el mapa para marcar la ubicación de la sala.');
      return;
    }

    // Construimos el GeoJSON automáticamente — el admin ya no lo escribe a mano
    const geomGeoJson = JSON.stringify({
      type: 'Point',
      coordinates: [selectedPoint.lng, selectedPoint.lat], // GeoJSON: [lng, lat]
    });

    setLoading(true);
    const payload = {
      ...form,
      buildingId: Number(form.buildingId),
      capacity: Number(form.capacity),
      geomGeoJson,
      ...(isEdit && { id: Number(id) }),
    };
    try {
      if (isEdit) {
        await api.put(`/api/rooms/${id}`, payload);
      } else {
        await api.post('/api/rooms', payload);
      }
      navigate('/rooms');
    } catch (err) {
      setError(err.response?.data || 'Error al guardar la sala.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container py-4" style={{ maxWidth: 640 }}>
      <div className="d-flex align-items-center gap-3 mb-1">
        <button className="btn btn-sm btn-outline-secondary" onClick={() => navigate('/rooms')}>
          ← Volver
        </button>
        <h2 className="fw-bold mb-0">{isEdit ? 'Editar Sala' : 'Nueva Sala'}</h2>
      </div>
      <p className="text-muted mb-4">
        {isEdit ? `Modificando sala #${id}` : 'Ingresa los datos de la nueva sala'}
      </p>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card shadow-sm border-0">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit}>
            <div className="row g-3">
              <div className="col-12">
                <label className="form-label fw-semibold">Edificio</label>
                <select name="buildingId" className="form-select"
                  value={form.buildingId} onChange={handleChange} required>
                  <option value="">— Selecciona un edificio —</option>
                  {buildings.map((b) => (
                    <option key={b.id} value={b.id}>{b.code} — {b.name}</option>
                  ))}
                </select>
              </div>
              <div className="col-sm-4">
                <label className="form-label fw-semibold">Código</label>
                <input type="text" name="code" className="form-control"
                  value={form.code} onChange={handleChange} placeholder="A-201" required />
              </div>
              <div className="col-sm-8">
                <label className="form-label fw-semibold">Nombre</label>
                <input type="text" name="name" className="form-control"
                  value={form.name} onChange={handleChange} placeholder="Sala 201" required />
              </div>
              <div className="col-sm-4">
                <label className="form-label fw-semibold">Capacidad</label>
                <input type="number" name="capacity" className="form-control" min="1"
                  value={form.capacity} onChange={handleChange} required />
              </div>

              <div className="col-12">
                <label className="form-label fw-semibold">Ubicación en el mapa</label>
                <p className="text-muted small mb-2">Haz clic en el mapa para marcar la ubicación exacta de la sala.</p>
                <MapView
                  onMapClick={handleMapClick}
                  pendingMarker={selectedPoint ? [selectedPoint.lat, selectedPoint.lng] : null}
                />
                {selectedPoint && (
                  <div className="alert alert-info py-2 mt-2 mb-0 small">
                    📍 Coordenada seleccionada: {selectedPoint.lat.toFixed(6)}, {selectedPoint.lng.toFixed(6)}
                  </div>
                )}
              </div>
            </div>

            <div className="d-flex gap-2 mt-4">
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Guardando...' : isEdit ? 'Guardar Cambios' : 'Crear Sala'}
              </button>
              <button type="button" className="btn btn-outline-secondary" onClick={() => navigate('/rooms')}>
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}