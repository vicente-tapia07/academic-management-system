import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../../services/api';

export default function CareerForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);

  const [form, setForm]     = useState({ code: '', name: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');

  useEffect(() => {
    if (!isEdit) return;
    api.get(`/api/careers/${id}`).then((r) => setForm({ code: r.data.code, name: r.data.name }));
  }, [id, isEdit]);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (isEdit) {
        await api.put(`/api/careers/${id}`, { ...form, id });
      } else {
        await api.post('/api/careers', form);
      }
      navigate('/careers');
    } catch (err) {
      setError(err.response?.data?.message || 'Error al guardar la carrera.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container py-4" style={{ maxWidth: 540 }}>
      <h2 className="fw-bold mb-1">{isEdit ? 'Editar Carrera' : 'Nueva Carrera'}</h2>
      <p className="text-muted mb-4">{isEdit ? `Modificando carrera #${id}` : 'Ingresa los datos de la nueva carrera'}</p>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card shadow-sm border-0">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit}>
            <div className="mb-3">
              <label className="form-label fw-semibold">Código</label>
              <input
                type="text" name="code" className="form-control"
                value={form.code} onChange={handleChange}
                placeholder="Ej: ING-INFO" required
              />
            </div>
            <div className="mb-4">
              <label className="form-label fw-semibold">Nombre</label>
              <input
                type="text" name="name" className="form-control"
                value={form.name} onChange={handleChange}
                placeholder="Ej: Ingeniería Informática" required
              />
            </div>
            <div className="d-flex gap-2">
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Guardando...' : isEdit ? 'Guardar Cambios' : 'Crear Carrera'}
              </button>
              <button type="button" className="btn btn-outline-secondary" onClick={() => navigate('/careers')}>
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}