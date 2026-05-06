import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../../services/api';

export default function SemesterClose() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [semester, setSemester] = useState(null);
  const [loading, setLoading]   = useState(false);
  const [done, setDone]         = useState(false);
  const [error, setError]       = useState('');

  useEffect(() => {
    api.get(`/api/semesters/${id}`).then((r) => setSemester(r.data)).catch(() => setError('No se encontró el semestre.'));
  }, [id]);

  const handleClose = async () => {
    setLoading(true);
    setError('');
    try {
      await api.post(`/api/semesters/${id}/close`);
      setDone(true);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al cerrar el semestre.');
    } finally {
      setLoading(false);
    }
  };

  if (done) {
    return (
      <div className="container py-5 text-center" style={{ maxWidth: 480 }}>
        <div className="mb-3" style={{ fontSize: '3rem' }}>✅</div>
        <h3 className="fw-bold">Semestre cerrado exitosamente</h3>
        <p className="text-muted">Las notas han sido congeladas y se calculó el Promedio Ponderado Acumulado de cada alumno.</p>
        <button className="btn btn-primary" onClick={() => navigate('/semesters')}>Volver a Semestres</button>
      </div>
    );
  }

  return (
    <div className="container py-4" style={{ maxWidth: 520 }}>
      <h2 className="fw-bold mb-1 text-danger">⚠️ Cerrar Semestre</h2>
      <p className="text-muted mb-4">Esta acción es irreversible</p>

      {error && <div className="alert alert-danger">{error}</div>}

      {semester && (
        <div className="card border-danger shadow-sm mb-4">
          <div className="card-header bg-danger text-white fw-semibold">
            Semestre a cerrar
          </div>
          <div className="card-body">
            <table className="table table-borderless mb-0 small">
              <tbody>
                <tr><th>Año</th><td>{semester.year}</td></tr>
                <tr><th>Período</th><td>{semester.period}</td></tr>
                <tr><th>Inicio</th><td>{semester.startDate}</td></tr>
                <tr><th>Fin</th><td>{semester.endDate}</td></tr>
                <tr><th>Estado actual</th><td><span className="badge bg-success">{semester.status}</span></td></tr>
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="alert alert-warning">
        <strong>¿Estás seguro?</strong> Al cerrar el semestre:
        <ul className="mb-0 mt-2">
          <li>Las notas quedarán congeladas</li>
          <li>Se calculará el PPA de cada estudiante</li>
          <li>Los alumnos con promedio bajo 4.0 cambiarán de estado</li>
        </ul>
      </div>

      <div className="d-flex gap-2">
        <button className="btn btn-danger" onClick={handleClose} disabled={loading || !semester}>
          {loading ? 'Cerrando semestre...' : 'Confirmar Cierre'}
        </button>
        <button className="btn btn-outline-secondary" onClick={() => navigate('/semesters')}>
          Cancelar
        </button>
      </div>
    </div>
  );
}