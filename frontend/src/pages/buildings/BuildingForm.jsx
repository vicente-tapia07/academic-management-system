import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../../services/api';
import MapView from '../../components/MapView';

export default function BuildingForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);

  const [form, setForm]   = useState({ code: '', name: '' });
  const [points, setPoints] = useState([]); // [{lat, lng}, ...]
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');

  useEffect(() => {
    if (!isEdit) return;
    api.get(`/api/buildings/${id}`).then((r) => {
      const { code, name, geomGeoJson } = r.data;
      setForm({ code, name });

      // Precargamos los puntos ya guardados para poder seguir editando el polígono
      const geom = JSON.parse(geomGeoJson);
      const ring = geom.coordinates[0]; // [[lng,lat], [lng,lat], ..., [lng,lat] (repetido)]
      // El último punto de un polígono GeoJSON repite el primero para "cerrar" la figura.
      // Lo quitamos, porque nosotros lo volvemos a agregar automáticamente al guardar.
      const withoutClosingPoint = ring.slice(0, -1);
      setPoints(withoutClosingPoint.map(([lng, lat]) => ({ lat, lng })));
    });
  }, [id, isEdit]);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleMapClick = ({ lat, lng }) => {
    setPoints((prev) => [...prev, { lat, lng }]);
    setError('');
  };

  const handleUndo = () => setPoints((prev) => prev.slice(0, -1));
  const handleClear = () => setPoints([]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (points.length < 3) {
      setError('Marca al menos 3 puntos en el mapa para formar el contorno del edificio.');
      return;
    }

    // Construimos el anillo GeoJSON: [lng, lat] por punto, y cerramos repitiendo el primero al final
    const ring = points.map((p) => [p.lng, p.lat]);
    ring.push(ring[0]);

    const geomGeoJson = JSON.stringify({ type: 'Polygon', coordinates: [ring] });

    setLoading(true);
    const payload = { ...form, geomGeoJson, ...(isEdit && { id: Number(id) }) };
    try {
      if (isEdit) {
        await api.put(`/api/buildings/${id}`, payload);
      } else {
        await api.post('/api/buildings', payload);
      }
      navigate('/buildings');
    } catch (err) {
      setError(err.response?.data || 'Error al guardar el edificio.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container py-4" style={{ maxWidth: 640 }}>
      <div className="d-flex align-items-center gap-3 mb-1">
        <button className="btn btn-sm btn-outline-secondary" onClick={() => navigate('/buildings')}>
          ← Volver
        </button>
        <h2 className="fw-bold mb-0">{isEdit ? 'Editar Edificio' : 'Nuevo Edificio'}</h2>
      </div>
      <p className="text-muted mb-4">
        {isEdit ? `Modificando edificio #${id}` : 'Ingresa los datos del nuevo edificio'}
      </p>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card shadow-sm border-0">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit}>
            <div className="row g-3">
              <div className="col-sm-4">
                <label className="form-label fw-semibold">Código</label>
                <input type="text" name="code" className="form-control"
                  value={form.code} onChange={handleChange} placeholder="FING" required />
              </div>
              <div className="col-sm-8">
                <label className="form-label fw-semibold">Nombre</label>
                <input type="text" name="name" className="form-control"
                  value={form.name} onChange={handleChange} placeholder="Facultad de Ingeniería" required />
              </div>

              <div className="col-12">
                <label className="form-label fw-semibold">Contorno del edificio</label>
                <p className="text-muted small mb-2">
                  Haz clic en el mapa para marcar cada esquina del edificio, en orden (mínimo 3 puntos).
                </p>
                <MapView onMapClick={handleMapClick} drawingPoints={points} />

                <div className="d-flex justify-content-between align-items-center mt-2">
                  <span className="text-muted small">
                    {points.length} punto{points.length !== 1 ? 's' : ''} marcado{points.length !== 1 ? 's' : ''}
                    {points.length < 3 && ' (mínimo 3)'}
                  </span>
                  <div className="d-flex gap-2">
                    <button type="button" className="btn btn-sm btn-outline-secondary"
                      onClick={handleUndo} disabled={points.length === 0}>
                      ↩ Deshacer último punto
                    </button>
                    <button type="button" className="btn btn-sm btn-outline-danger"
                      onClick={handleClear} disabled={points.length === 0}>
                      🗑 Limpiar figura
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div className="d-flex gap-2 mt-4">
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Guardando...' : isEdit ? 'Guardar Cambios' : 'Crear Edificio'}
              </button>
              <button type="button" className="btn btn-outline-secondary" onClick={() => navigate('/buildings')}>
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}