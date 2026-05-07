import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';

export default function SectionForm() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    subjectId: '', professorId: '', semesterId: '',
    totalSeats: '', availableSeats: '',
  });
  const [subjects,   setSubjects]   = useState([]);
  const [semesters,  setSemesters]  = useState([]);
  const [professors, setProfessors] = useState([]);
  const [loading,    setLoading]    = useState(false);
  const [error,      setError]      = useState('');

  useEffect(() => {
    Promise.all([
      api.get('/api/subjects'),
      api.get('/api/semesters'),
      api.get('/api/professors'),
    ]).then(([sRes, semRes, profRes]) => {
      setSubjects(sRes.data);
      setSemesters(semRes.data);
      setProfessors(profRes.data);
    });
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
  };

  const handleTotalSeats = (e) => {
    const val = e.target.value;
    setForm({ ...form, totalSeats: val, availableSeats: val });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    const payload = {
      subjectId:      Number(form.subjectId),
      professorId:    Number(form.professorId),
      semesterId:     Number(form.semesterId),
      totalSeats:     Number(form.totalSeats),
      availableSeats: Number(form.availableSeats),
    };
    try {
      await api.post('/api/sections', payload);
      navigate('/sections');
    } catch (err) {
      setError(err.response?.data?.message || 'Error al crear la sección.');
    } finally {
      setLoading(false);
    }
  };

  // Badge de estado para el dropdown de semestres
  const statusLabel = (status) => {
    if (status === 'IN_PROGRESS') return ' ✅ En curso';
    if (status === 'CLOSED')      return ' 🔒 Cerrado';
    return ' 📅 Planificado';
  };

  return (
    <div className="container py-4" style={{ maxWidth: 560 }}>
      <h2 className="fw-bold mb-1">Nueva Sección</h2>
      <p className="text-muted mb-4">Crea una nueva sección para una asignatura</p>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card shadow-sm border-0">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit}>
            <div className="row g-3">

              <div className="col-12">
                <label className="form-label fw-semibold">Asignatura</label>
                <select name="subjectId" className="form-select"
                  value={form.subjectId} onChange={handleChange} required>
                  <option value="">— Selecciona asignatura —</option>
                  {subjects.map((s) => (
                    <option key={s.id} value={s.id}>{s.code} — {s.name}</option>
                  ))}
                </select>
              </div>

              <div className="col-12">
                <label className="form-label fw-semibold">Semestre</label>
                <select name="semesterId" className="form-select"
                  value={form.semesterId} onChange={handleChange} required>
                  <option value="">— Selecciona semestre —</option>
                  {semesters.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.year} — {s.period}{statusLabel(s.status)}
                    </option>
                  ))}
                </select>
                <div className="form-text">Se recomienda asignar secciones al semestre en curso.</div>
              </div>

              <div className="col-12">
                <label className="form-label fw-semibold">Profesor</label>
                <select name="professorId" className="form-select"
                  value={form.professorId} onChange={handleChange} required>
                  <option value="">— Selecciona profesor —</option>
                  {professors.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.firstName} {p.lastName} — {p.department}
                    </option>
                  ))}
                </select>
              </div>

              <div className="col-sm-6">
                <label className="form-label fw-semibold">Cupos Totales</label>
                <input type="number" name="totalSeats" className="form-control" min="1"
                  value={form.totalSeats} onChange={handleTotalSeats} required />
              </div>

              <div className="col-sm-6">
                <label className="form-label fw-semibold">Cupos Disponibles</label>
                <input type="number" name="availableSeats" className="form-control" min="0"
                  value={form.availableSeats} onChange={handleChange} required />
                <div className="form-text">Normalmente igual a cupos totales.</div>
              </div>

            </div>

            <div className="d-flex gap-2 mt-4">
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Creando...' : 'Crear Sección'}
              </button>
              <button type="button" className="btn btn-outline-secondary"
                onClick={() => navigate('/sections')}>
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}