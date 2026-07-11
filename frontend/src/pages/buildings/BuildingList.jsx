import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../services/api';

export default function BuildingList() {
  const [buildings, setBuildings] = useState([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');

  const load = async () => {
    try {
      setLoading(true);
      const res = await api.get('/api/buildings');
      setBuildings(res.data);
    } catch {
      setError('Error al cargar los edificios.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleDelete = async (id) => {
    if (!window.confirm('¿Eliminar este edificio? También se eliminarán sus salas.')) return;
    try {
      await api.delete(`/api/buildings/${id}`);
      load();
    } catch {
      alert('Error al eliminar el edificio.');
    }
  };

  const navigate = useNavigate();

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div className="d-flex align-items-center gap-3">
          <button className="btn btn-sm btn-outline-secondary" onClick={() => navigate('/dashboard')}>
            ← Volver
          </button>
          <div>
            <h2 className="fw-bold mb-0">Edificios</h2>
            <p className="text-muted mb-0">Gestiona los edificios del campus</p>
          </div>
        </div>
        <Link to="/buildings/new" className="btn btn-primary">+ Nuevo Edificio</Link>
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
                  <th>Salas</th>
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {buildings.length === 0 && (
                  <tr><td colSpan={4} className="text-center text-muted py-4">No hay edificios registrados</td></tr>
                )}
                {buildings.map((b) => (
                  <tr key={b.id}>
                    <td><span className="badge bg-primary">{b.code}</span></td>
                    <td className="fw-semibold">{b.name}</td>
                    <td>
                      <Link to={`/rooms?buildingId=${b.id}`} className="text-decoration-none small">
                        Ver salas →
                      </Link>
                    </td>
                    <td className="text-end">
                      <Link to={`/buildings/edit/${b.id}`} className="btn btn-sm btn-outline-primary me-2">
                        Editar
                      </Link>
                      <button className="btn btn-sm btn-outline-danger" onClick={() => handleDelete(b.id)}>
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