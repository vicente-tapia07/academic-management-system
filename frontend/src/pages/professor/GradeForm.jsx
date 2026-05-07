import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import api from '../../services/api';

export default function GradeForm() {
  const navigate = useNavigate();
  const { state } = useLocation();

  if (!state?.enrollmentId) {
    navigate('/professor');
    return null;
  }

  const { enrollmentId, studentName, existingGrade } = state;
  const isEdit = Boolean(existingGrade);

  const [value,   setValue]   = useState(existingGrade?.value?.toString() ?? '');
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');
  const [success, setSuccess] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    const numValue = parseFloat(value);
    if (isNaN(numValue) || numValue < 1.0 || numValue > 7.0) {
      setError('La nota debe estar entre 1.0 y 7.0');
      setLoading(false);
      return;
    }

    const payload = {
      enrollmentId,
      value: numValue,
      entryDate: new Date().toISOString().split('T')[0], // fecha actual YYYY-MM-DD
    };

    try {
      if (isEdit) {
        await api.put(`/api/grades/${existingGrade.id}`, payload);
      } else {
        await api.post('/api/grades', payload);
      }
      setSuccess('Nota guardada correctamente.');
      setTimeout(() => navigate(-1), 1500);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al guardar la nota.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container py-4" style={{ maxWidth: 480 }}>
      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-outline-secondary" onClick={() => navigate(-1)}>
          ← Volver
        </button>
        <div>
          <h2 className="fw-bold mb-0">{isEdit ? 'Editar Nota' : 'Ingresar Nota'}</h2>
          <p className="text-muted mb-0">{studentName}</p>
        </div>
      </div>

      {error   && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="card shadow-sm border-0">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit}>
            <div className="mb-3">
              <label className="form-label fw-semibold">
                Nota (1.0 – 7.0)
              </label>
              <input
                type="number"
                className="form-control form-control-lg"
                min="1.0" max="7.0" step="0.1"
                value={value}
                onChange={(e) => setValue(e.target.value)}
                required
              />
              <div className="form-text">
                Nota de aprobación: 4.0
              </div>
            </div>

            {/* Vista previa del resultado */}
            {value && !isNaN(parseFloat(value)) && (
              <div className={`alert ${parseFloat(value) >= 4.0 ? 'alert-success' : 'alert-danger'}`}>
                {parseFloat(value) >= 4.0 ? '✅ Aprobado' : '❌ Reprobado'}
              </div>
            )}

            <div className="d-flex gap-2 mt-3">
              <button type="submit" className="btn btn-primary flex-grow-1" disabled={loading}>
                {loading ? 'Guardando...' : isEdit ? 'Guardar Cambios' : 'Ingresar Nota'}
              </button>
              <button type="button" className="btn btn-outline-secondary"
                onClick={() => navigate(-1)}>
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}