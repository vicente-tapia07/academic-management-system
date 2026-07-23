import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../../services/api';

const STATUS_OPTIONS = ['ACTIVE', 'BLOCKED', 'GRADUATED'];

export default function StudentForm() {
  const navigate = useNavigate();
  const { id }   = useParams();
  const isEdit   = Boolean(id);

  const [form, setForm] = useState({
    rut: '', email: '', password: '',
    firstName: '', lastName: '', enrollmentNumber: '',
    academicStatus: 'ACTIVE',
  });
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');

  useEffect(() => {
    if (!isEdit) return;
    api.get(`/api/students/${id}`).then((r) => {
      const s = r.data;
      setForm({
        rut:              '',  // no se devuelve por seguridad
        email:            '',  // no se devuelve por seguridad
        password:         '',
        firstName:        s.firstName        ?? '',
        lastName:         s.lastName         ?? '',
        enrollmentNumber: s.enrollmentNumber ?? '',
        academicStatus:   s.academicStatus   ?? 'ACTIVE',
      });
    });
  }, [id, isEdit]);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (isEdit) {
        // PUT actualiza nombre, apellido y estado
        await api.put(`/api/students/${id}`, {
          firstName:      form.firstName,
          lastName:       form.lastName,
          academicStatus: form.academicStatus,
        });
      } else {
        await api.post('/api/students', form);
      }
      navigate('/students');
    } catch (err) {
      setError(err.response?.data || 'Error al guardar el estudiante.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container py-4" style={{ maxWidth: 560 }}>
      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-outline-secondary" onClick={() => navigate('/students')}>
          ← Volver
        </button>
        <div>
          <h2 className="fw-bold mb-0">{isEdit ? 'Editar Estudiante' : 'Nuevo Estudiante'}</h2>
          <p className="text-muted mb-0">
            {isEdit ? `Modificando estudiante #${id}` : 'Crear cuenta y perfil de estudiante'}
          </p>
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card shadow-sm border-0">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit}>
            <div className="row g-3">

              <div className="col-sm-6">
                <label className="form-label fw-semibold">Nombre</label>
                <input type="text" name="firstName" className="form-control"
                  value={form.firstName} onChange={handleChange}
                  placeholder="Juan" required />
              </div>
              <div className="col-sm-6">
                <label className="form-label fw-semibold">Apellido</label>
                <input type="text" name="lastName" className="form-control"
                  value={form.lastName} onChange={handleChange}
                  placeholder="Pérez" required />
              </div>

              {/* Solo al crear */}
              {!isEdit && (
                <>
                  <div className="col-sm-5">
                    <label className="form-label fw-semibold">RUT</label>
                    <input type="text" name="rut" className="form-control"
                      value={form.rut} onChange={handleChange}
                      placeholder="12345678-9" required />
                  </div>
                  <div className="col-sm-7">
                    <label className="form-label fw-semibold">Email</label>
                    <input type="email" name="email" className="form-control"
                      value={form.email} onChange={handleChange}
                      placeholder="juan@usach.cl" required />
                  </div>
                  <div className="col-sm-6">
                    <label className="form-label fw-semibold">Contraseña</label>
                    <input type="password" name="password" className="form-control"
                      value={form.password} onChange={handleChange}
                      placeholder="••••••••" required />
                  </div>
                  <div className="col-sm-6">
                    <label className="form-label fw-semibold">N° Matrícula</label>
                    <input type="text" name="enrollmentNumber" className="form-control"
                      value={form.enrollmentNumber} onChange={handleChange}
                      placeholder="2026001" required />
                  </div>
                </>
              )}

              {/* Solo al editar: cambiar estado académico */}
              {isEdit && (
                <div className="col-12">
                  <label className="form-label fw-semibold">Estado académico</label>
                  <select name="academicStatus" className="form-select"
                    value={form.academicStatus} onChange={handleChange}>
                    {STATUS_OPTIONS.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                  <div className="form-text">
                    BLOCKED: el estudiante reprobó el semestre. GRADUATED: egresado.
                  </div>
                </div>
              )}

            </div>

            <div className="d-flex gap-2 mt-4">
              <button type="submit" className="btn btn-primary flex-grow-1" disabled={loading}>
                {loading ? 'Guardando...' : isEdit ? 'Guardar Cambios' : 'Crear Estudiante'}
              </button>
              <button type="button" className="btn btn-outline-secondary"
                onClick={() => navigate('/students')}>Cancelar</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
