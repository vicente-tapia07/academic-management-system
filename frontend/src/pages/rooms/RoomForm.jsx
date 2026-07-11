import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../../services/api';

export default function RoomForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);

  const [form, setForm] = useState({
    buildingId: '', code: '', name: '', capacity: '', geomGeoJson: '',
  });
  const [buildings, setBuildings] = useState([]);
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState('');

  useEffect(() => {
    api.get('/api/buildings').then((r) => setBuildings(r.data));
    if (!isEdit) return;
    api.get(`/api/rooms/${id}`).then((r) => {
      const { buildingId, code, name, capacity, geomGeoJson } = r.data;
      setForm({ buildingId, code, name, capacity, geomGeoJson });
    });
  }, [id, isEdit]);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    try {
      const parsed = JSON.parse(form.geomGeoJson);
      if (parsed.type !== 'Point') {
        setError('La geometría debe ser de tipo Point.');
        return;
      }
    } catch {
      setError('El GeoJSON ingresado no es un JSON válido. Revisa el formato.');
      return;
    }

    setLoading(true);
    const payload = {
      ...form,
      buildingId: Number(form.buildingId),
      capacity: Number(form.capacity),
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
                <label className="form-label fw-semibold">Geometría (GeoJSON — Point)</label>
                <textarea name="geomGeoJson" className="form-control font-monospace small" rows={2}
                  value={form.geomGeoJson} onChange={handleChange}
                  placeholder='{"type":"Point","coordinates":[-70.6845,-33.4487]}'
                  required />
                <div className="form-text text-muted">
                  Debe ser un GeoJSON de tipo Point, con la coordenada exacta de la sala.
                </div>
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