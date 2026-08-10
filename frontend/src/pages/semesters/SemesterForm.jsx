import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../../services/api';

const empty = {
  year: new Date().getFullYear(),
  period: '1S',
  startDate: '', endDate: '',
  gradeStartDate: '', gradeEndDate: '',
  status: 'PLANNED',          // ← valor correcto por defecto
};

export default function SemesterForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);

  const [form, setForm]       = useState(empty);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');

  useEffect(() => {
    if (!isEdit) return;
    api.get(`/api/semesters/${id}`).then((r) => {
      const { year, period, startDate, endDate, gradeStartDate, gradeEndDate, status } = r.data;
      setForm({ year, period, startDate, endDate, gradeStartDate, gradeEndDate, status });
    });
  }, [id, isEdit]);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    const payload = { ...form, year: Number(form.year), ...(isEdit && { id }) };
    try {
      if (isEdit) {
        await api.put(`/api/semesters/${id}`, payload);
      } else {
        await api.post('/api/semesters', payload);
      }
      navigate('/semesters');
    } catch (err) {
      setError(err.response?.data?.message || 'Error al guardar el semestre.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container py-4" style={{ maxWidth: 600 }}>
      <h2 className="fw-bold mb-1">{isEdit ? 'Editar Semestre' : 'Nuevo Semestre'}</h2>
      <p className="text-muted mb-4">
        {isEdit ? `Modificando semestre #${id}` : 'Define el nuevo período académico'}
      </p>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card shadow-sm border-0">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit}>
            <div className="row g-3">

              <div className="col-sm-6">
                <label className="form-label fw-semibold">Año</label>
                <input type="number" name="year" className="form-control"
                  min="2020" max="2035"
                  value={form.year} onChange={handleChange} required />
              </div>

              <div className="col-sm-6">
                <label className="form-label fw-semibold">Período</label>
                <select name="period" className="form-select"
                  value={form.period} onChange={handleChange} required>
                  <option value="1S">Primer semestre (1S)</option>
                  <option value="2S">Segundo semestre (2S)</option>
                </select>
              </div>

              <div className="col-12">
                <hr className="my-1" />
                <small className="text-muted fw-semibold">FECHAS DEL SEMESTRE</small>
              </div>

              <div className="col-sm-6">
                <label className="form-label fw-semibold">Fecha Inicio</label>
                <input type="date" name="startDate" className="form-control"
                  value={form.startDate} onChange={handleChange} required />
              </div>
              <div className="col-sm-6">
                <label className="form-label fw-semibold">Fecha Fin</label>
                <input type="date" name="endDate" className="form-control"
                  value={form.endDate} onChange={handleChange} required />
              </div>

              <div className="col-12">
                <hr className="my-1" />
                <small className="text-muted fw-semibold">PERÍODO DE INGRESO DE NOTAS</small>
              </div>

              <div className="col-sm-6">
                <label className="form-label fw-semibold">Inicio Notas</label>
                <input type="date" name="gradeStartDate" className="form-control"
                  value={form.gradeStartDate} onChange={handleChange} required />
              </div>
              <div className="col-sm-6">
                <label className="form-label fw-semibold">Fin Notas</label>
                <input type="date" name="gradeEndDate" className="form-control"
                  value={form.gradeEndDate} onChange={handleChange} required />
              </div>

              <div className="col-12">
                <label className="form-label fw-semibold">Estado</label>
                <select name="status" className="form-select"
                  value={form.status} onChange={handleChange}>
                  <option value="PLANNED">Planificado</option>
                  <option value="IN_PROGRESS">En Curso</option>
                  <option value="CLOSED">Cerrado</option>
                </select>
                <div className="form-text text-muted">
                  Solo puede haber un semestre <strong>En Curso</strong> a la vez.
                </div>
              </div>

            </div>

            <div className="d-flex gap-2 mt-4">
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Guardando...' : isEdit ? 'Guardar Cambios' : 'Crear Semestre'}
              </button>
              <button type="button" className="btn btn-outline-secondary"
                onClick={() => navigate('/semesters')}>
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}