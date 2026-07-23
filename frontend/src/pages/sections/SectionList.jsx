import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../services/api';

const DAY_NAMES = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

export default function SectionList() {
  const navigate = useNavigate();
  const [sections,   setSections]   = useState([]);
  const [subjects,   setSubjects]   = useState([]);
  const [semesters,  setSemesters]  = useState([]);
  const [rooms,      setRooms]      = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [error,      setError]      = useState('');

  const load = async () => {
    try {
      setLoading(true);
      const [secRes, subRes, semRes, roomRes] = await Promise.all([
        api.get('/api/sections'),
        api.get('/api/subjects'),
        api.get('/api/semesters'),
        api.get('/api/rooms'),
      ]);
      setSections(secRes.data);
      setSubjects(subRes.data);
      setSemesters(semRes.data);
      setRooms(roomRes.data);
    } catch {
      setError('Error al cargar las secciones.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const subjectName = (id) => subjects.find((s) => s.id === id)?.name ?? `#${id}`;
  const subjectCode = (id) => subjects.find((s) => s.id === id)?.code ?? '---';
  const roomName    = (id) => rooms.find((r) => r.id === id)?.name ?? (id ? `Sala #${id}` : '—');

  const semesterBadge = (id) => {
    const s = semesters.find((s) => s.id === id);
    if (!s) return `#${id}`;
    const cls = s.status === 'IN_PROGRESS' ? 'bg-success' :
                s.status === 'CLOSED'      ? 'bg-dark'    : 'bg-secondary';
    return <span className={`badge ${cls}`}>{s.year}-{s.period}</span>;
  };

  const handleDelete = async (id) => {
    if (!window.confirm('¿Eliminar esta sección? Esta acción no se puede deshacer.')) return;
    try {
      await api.delete(`/api/sections/${id}`);
      load();
    } catch (err) {
      alert(err.response?.data || 'Error al eliminar la sección.');
    }
  };

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <button className="btn btn-outline-secondary" onClick={() => navigate('/dashboard')}>
          ← Volver
        </button>
        <div>
          <h2 className="fw-bold mb-0">Secciones</h2>
          <p className="text-muted mb-0">Secciones por semestre, sala y horario</p>
        </div>
        <Link to="/sections/new" className="btn btn-primary">+ Nueva Sección</Link>
      </div>

      {loading && <p className="text-muted">Cargando...</p>}
      {error   && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && (
        <div className="card shadow-sm border-0">
          <div className="table-responsive">
            <table className="table table-hover mb-0 align-middle">
              <thead className="table-light">
                <tr>
                  <th style={{ width: 50 }}>#</th>
                  <th>Asignatura</th>
                  <th>Semestre</th>
                  <th>Sala</th>
                  <th>Horario</th>
                  <th style={{ width: 80 }}>Cupos</th>
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {sections.length === 0 && (
                  <tr><td colSpan={7} className="text-center text-muted py-4">No hay secciones registradas</td></tr>
                )}
                {sections.map((s) => (
                  <tr key={s.id}>
                    <td className="text-muted small">{s.id}</td>
                    <td>
                      <span className="badge bg-primary font-monospace me-1">{subjectCode(s.subjectId)}</span>
                      <span className="fw-semibold small">{subjectName(s.subjectId)}</span>
                    </td>
                    <td>{semesterBadge(s.semesterId)}</td>
                    <td className="small text-muted">{roomName(s.roomId)}</td>
                    <td className="small text-muted text-nowrap">
                      {s.dayOfWeek != null
                        ? `${DAY_NAMES[s.dayOfWeek]} ${s.startTime?.slice(0,5) ?? ''}–${s.endTime?.slice(0,5) ?? ''}`
                        : '—'}
                    </td>
                    <td>
                      <span className={s.availableSeats === 0 ? 'text-danger fw-bold' : 'text-success fw-bold'}>
                        {s.availableSeats}
                      </span>
                      <span className="text-muted small">/{s.totalSeats}</span>
                    </td>
                    <td className="text-end">
                      <Link to={`/sections/edit/${s.id}`} className="btn btn-sm btn-outline-primary me-1">
                        Editar
                      </Link>
                      <button className="btn btn-sm btn-outline-danger"
                        onClick={() => handleDelete(s.id)}>
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
