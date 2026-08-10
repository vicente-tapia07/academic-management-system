import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../../services/api';

export default function SubjectForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);

  const [form, setForm]     = useState({ code: '', name: '', credits: '', careerId: '', active: true });
  const [careers, setCareers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');

  useEffect(() => {
    api.get('/api/careers').then((r) => setCareers(r.data));
    if (!isEdit) return;
    api.get(`/api/subjects/${id}`).then((r) => {
      const { code, name, credits, careerId, active } = r.data;
      setForm({ code, name, credits, careerId, active });
    });
  }, [id, isEdit]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    const payload = {
      ...form,
      credits: Number(form.credits),
      careerId: form.careerId,
      ...(isEdit && { id }),
    };
    try {
      if (isEdit) {
        await api.put(`/api/subjects/${id}`, payload);
      } else {
        await api.post('/api/subjects', payload);
      }
      navigate('/subjects');
    } catch (err) {
      setError(err.response?.data?.message || 'Error al guardar la asignatura.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container py-4" style={{ maxWidth: 560 }}>
      <h2 className="fw-bold mb-1">{isEdit ? 'Editar Asignatura' : 'Nueva Asignatura'}</h2>
      <p className="text-muted mb-4">{isEdit ? `Modificando asignatura #${id}` : 'Ingresa los datos de la nueva asignatura'}</p>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card shadow-sm border-0">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit}>
            <div className="row g-3">
              <div className="col-sm-4">
                <label className="form-label fw-semibold">Código</label>
                <input type="text" name="code" className="form-control"
                  value={form.code} onChange={handleChange} placeholder="CAL1" required />
              </div>
              <div className="col-sm-8">
                <label className="form-label fw-semibold">Nombre</label>
                <input type="text" name="name" className="form-control"
                  value={form.name} onChange={handleChange} placeholder="Cálculo 1" required />
              </div>
              <div className="col-sm-4">
                <label className="form-label fw-semibold">Créditos</label>
                <input type="number" name="credits" className="form-control" min="1" max="20"
                  value={form.credits} onChange={handleChange} required />
              </div>
              <div className="col-sm-8">
                <label className="form-label fw-semibold">Carrera</label>
                <select name="careerId" className="form-select"
                  value={form.careerId} onChange={handleChange} required>
                  <option value="">— Selecciona una carrera —</option>
                  {careers.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
              <div className="col-12">
                <div className="form-check">
                  <input className="form-check-input" type="checkbox" name="active"
                    checked={form.active} onChange={handleChange} id="activeCheck" />
                  <label className="form-check-label" htmlFor="activeCheck">
                    Asignatura activa
                  </label>
                </div>
              </div>
            </div>

            <div className="d-flex gap-2 mt-4">
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Guardando...' : isEdit ? 'Guardar Cambios' : 'Crear Asignatura'}
              </button>
              <button type="button" className="btn btn-outline-secondary" onClick={() => navigate('/subjects')}>
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}