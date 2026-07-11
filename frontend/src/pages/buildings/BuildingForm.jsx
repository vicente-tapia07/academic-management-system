import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../../services/api';

export default function BuildingForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);

  const [form, setForm]       = useState({ code: '', name: '', geomGeoJson: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');

  useEffect(() => {
    if (!isEdit) return;
    api.get(`/api/buildings/${id}`).then((r) => {
      const { code, name, geomGeoJson } = r.data;
      setForm({ code, name, geomGeoJson });
    });
  }, [id, isEdit]);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Validar que el GeoJSON sea texto válido antes de mandarlo
    try {
      const parsed = JSON.parse(form.geomGeoJson);
      if (parsed.type !== 'Polygon') {
        setError('La geometría debe ser de tipo Polygon.');
        return;
      }
    } catch {
      setError('El GeoJSON ingresado no es un JSON válido. Revisa el formato.');
      return;
    }

    setLoading(true);
    const payload = { ...form, ...(isEdit && { id: Number(id) }) };
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
                <label className="form-label fw-semibold">Geometría (GeoJSON — Polygon)</label>
                <textarea name="geomGeoJson" className="form-control font-monospace small" rows={4}
                  value={form.geomGeoJson} onChange={handleChange}
                  placeholder='{"type":"Polygon","coordinates":[[[-70.68,-33.44],[-70.68,-33.44],...]]}'
                  required />
                <div className="form-text text-muted">
                  Debe ser un GeoJSON de tipo Polygon, con las coordenadas del contorno del edificio.
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