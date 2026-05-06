import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../services/api';

export default function SubjectList() {
  const [subjects, setSubjects] = useState([]);
  const [careers, setCareers]   = useState([]);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState('');
  const navigate = useNavigate();

  const load = async () => {
    try {
      setLoading(true);
      const [sRes, cRes] = await Promise.all([
        api.get('/api/subjects'),
        api.get('/api/careers'),
      ]);
      setSubjects(sRes.data);
      setCareers(cRes.data);
    } catch {
      setError('Error al cargar las asignaturas.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const careerName = (id) => careers.find((c) => c.id === id)?.name ?? `#${id}`;

  const handleDelete = async (id) => {
    if (!window.confirm('¿Eliminar esta asignatura?')) return;
    try {
      await api.delete(`/api/subjects/${id}`);
      load();
    } catch {
      alert('Error al eliminar la asignatura.');
    }
  };

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div className="d-flex align-items-center gap-3">
          <button className="btn btn-outline-secondary" onClick={() => navigate('/dashboard')}>
            ← Volver
          </button>
        </div>
        <div>
          <h2 className="fw-bold mb-0">Asignaturas</h2>
          <p className="text-muted mb-0">Gestiona el catálogo de asignaturas</p>
        </div>
        <Link to="/subjects/new" className="btn btn-primary">+ Nueva Asignatura</Link>
      </div>

      {loading && <p className="text-muted">Cargando...</p>}
      {error   && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && (
        <div className="card shadow-sm border-0">
          <div className="table-responsive">
            <table className="table table-hover mb-0">
              <thead className="table-light">
                <tr>
                  <th>Código</th>
                  <th>Nombre</th>
                  <th>Créditos</th>
                  <th>Carrera</th>
                  <th>Estado</th>
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {subjects.length === 0 && (
                  <tr><td colSpan={6} className="text-center text-muted py-4">No hay asignaturas registradas</td></tr>
                )}
                {subjects.map((s) => (
                  <tr key={s.id}>
                    <td><span className="badge bg-primary">{s.code}</span></td>
                    <td className="fw-semibold">{s.name}</td>
                    <td>{s.credits}</td>
                    <td className="text-muted small">{careerName(s.careerId)}</td>
                    <td>
                      <span className={`badge ${s.active ? 'bg-success' : 'bg-secondary'}`}>
                        {s.active ? 'Activa' : 'Inactiva'}
                      </span>
                    </td>
                    <td className="text-end">
                      <Link to={`/subjects/edit/${s.id}`} className="btn btn-sm btn-outline-primary me-2">
                        Editar
                      </Link>
                      <button className="btn btn-sm btn-outline-danger" onClick={() => handleDelete(s.id)}>
                        Eliminar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}