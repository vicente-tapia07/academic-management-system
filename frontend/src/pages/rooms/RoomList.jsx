import React, { useEffect, useState } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import api from '../../services/api';

export default function RoomList() {
  const [searchParams] = useSearchParams();
  const buildingIdFilter = searchParams.get('buildingId');

  const [rooms, setRooms]         = useState([]);
  const [buildings, setBuildings] = useState([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');

  const load = async () => {
    try {
      setLoading(true);
      const url = buildingIdFilter
        ? `/api/rooms?buildingId=${buildingIdFilter}`
        : '/api/rooms';
      const [roomRes, buildRes] = await Promise.all([
        api.get(url),
        api.get('/api/buildings'),
      ]);
      setRooms(roomRes.data);
      setBuildings(buildRes.data);
    } catch {
      setError('Error al cargar las salas.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [buildingIdFilter]);

  const buildingName = (id) => buildings.find((b) => b.id === id)?.name ?? `#${id}`;

  const handleDelete = async (id) => {
    if (!window.confirm('¿Eliminar esta sala?')) return;
    try {
      await api.delete(`/api/rooms/${id}`);
      load();
    } catch {
      alert('Error al eliminar la sala.');
    }
  };

  const navigate = useNavigate();

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div className="d-flex align-items-center gap-3">
          <button className="btn btn-sm btn-outline-secondary" onClick={() => navigate('/buildings')}>
            ← Volver
          </button>
          <div>
            <h2 className="fw-bold mb-0">Salas</h2>
            <p className="text-muted mb-0">
              {buildingIdFilter ? `Salas de ${buildingName(Number(buildingIdFilter))}` : 'Todas las salas del campus'}
            </p>
          </div>
        </div>
        <Link to="/rooms/new" className="btn btn-primary">+ Nueva Sala</Link>
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
                  <th>Edificio</th>
                  <th>Capacidad</th>
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {rooms.length === 0 && (
                  <tr><td colSpan={5} className="text-center text-muted py-4">No hay salas registradas</td></tr>
                )}
                {rooms.map((r) => (
                  <tr key={r.id}>
                    <td><span className="badge bg-secondary">{r.code}</span></td>
                    <td className="fw-semibold">{r.name}</td>
                    <td className="text-muted small">{buildingName(r.buildingId)}</td>
                    <td>{r.capacity}</td>
                    <td className="text-end">
                      <Link to={`/rooms/edit/${r.id}`} className="btn btn-sm btn-outline-primary me-2">
                        Editar
                      </Link>
                      <button className="btn btn-sm btn-outline-danger" onClick={() => handleDelete(r.id)}>
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