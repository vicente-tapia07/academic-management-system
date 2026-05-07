import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';

export default function StudentForm() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    rut: '', email: '', password: '',
    firstName: '', lastName: '', enrollmentNumber: ''
  });
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await api.post('/api/students', form);
      navigate('/students');
    } catch (err) {
      setError(err.response?.data || 'Error al crear el estudiante.');
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
          <h2 className="fw-bold mb-0">Nuevo Estudiante</h2>
          <p className="text-muted mb-0">Crear cuenta y perfil de estudiante</p>
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
            </div>

            <div className="d-flex gap-2 mt-4">
              <button type="submit" className="btn btn-primary flex-grow-1" disabled={loading}>
                {loading ? 'Creando...' : 'Crear Estudiante'}
              </button>
              <button type="button" className="btn btn-outline-secondary"
                onClick={() => navigate('/students')}>
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}