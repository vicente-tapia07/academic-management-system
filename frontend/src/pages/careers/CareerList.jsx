import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../services/api';

export default function CareerList() {
  const [careers, setCareers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState('');
  const navigate = useNavigate();
  const load = async () => {
    try {
      setLoading(true);
      const res = await api.get('/api/careers');
      setCareers(res.data);
    } catch {
      setError('Error al cargar carreras.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleDelete = async (id) => {
    if (!window.confirm('¿Eliminar esta carrera?')) return;
    try {
      await api.delete(`/api/careers/${id}`);
      load();
    } catch {
      alert('Error al eliminar la carrera.');
    }
  };

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <button className="btn btn-outline-secondary me-3" onClick={() => navigate('/dashboard')}>
          ← Volver
        </button>
        <div>
          <h2 className="fw-bold mb-0">Carreras</h2>
          <p className="text-muted mb-0">Administra las carreras universitarias</p>
        </div>
        <Link to="/careers/new" className="btn btn-primary">+ Nueva Carrera</Link>
      </div>

      {loading && <p className="text-muted">Cargando...</p>}
      {error   && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && (
        <div className="card shadow-sm border-0">
          <div className="table-responsive">
            <table className="table table-hover mb-0">
              <thead className="table-light">
                <tr>
                  <th>ID</th>
                  <th>Código</th>
                  <th>Nombre</th>
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {careers.length === 0 && (
                  <tr><td colSpan={4} className="text-center text-muted py-4">No hay carreras registradas</td></tr>
                )}
                {careers.map((c) => (
                  <tr key={c.id}>
                    <td className="text-muted small">{c.id}</td>
                    <td><span className="badge bg-secondary">{c.code}</span></td>
                    <td className="fw-semibold">{c.name}</td>
                    <td className="text-end">
                      <Link to={`/careers/edit/${c.id}`} className="btn btn-sm btn-outline-primary me-2">
                        Editar
                      </Link>
                      <button className="btn btn-sm btn-outline-danger" onClick={() => handleDelete(c.id)}>
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
