import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../../services/api';

const DAY_NAMES = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

export default function ProfessorCoursesAdmin() {
  const { id }   = useParams(); // professor id
  const navigate = useNavigate();

  const [professor,  setProfessor]  = useState(null);
  const [sections,   setSections]   = useState([]);
  const [subjects,   setSubjects]   = useState([]);
  const [rooms,      setRooms]      = useState([]);
  const [semesters,  setSemesters]  = useState([]);
  const [filter,     setFilter]     = useState('active'); // 'active' | 'all'
  const [loading,    setLoading]    = useState(true);
  const [error,      setError]      = useState('');

  const loadData = async () => {
    setLoading(true);
    try {
      const [profRes, secRes, subRes, roomRes, semRes] = await Promise.all([
        api.get(`/api/professors/${id}`),
        api.get(`/api/professors/${id}/sections`),
        api.get('/api/subjects'),
        api.get('/api/rooms'),
        api.get('/api/semesters'),
      ]);
      setProfessor(profRes.data);
      setSections(secRes.data);
      setSubjects(subRes.data);
      setRooms(roomRes.data);
      setSemesters(semRes.data);
    } catch {
      setError('Error al cargar los cursos del profesor.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, [id]);

  const subjectName  = (sid) => subjects.find((s) => s.id === sid)?.name ?? `Asignatura #${sid}`;
  const subjectCode  = (sid) => subjects.find((s) => s.id === sid)?.code ?? '---';
  const roomName     = (rid) => rooms.find((r) => r.id === rid)?.name ?? (rid ? `Sala #${rid}` : '—');
  const getSemester  = (sid) => semesters.find((s) => s.id === sid);
  const activeSemId  = semesters.find((s) => s.status === 'IN_PROGRESS')?.id;

  const semesterBadge = (sem) => {
    if (!sem) return <span className="badge bg-secondary">—</span>;
    if (sem.status === 'CLOSED')      return <span className="badge bg-dark">🔒 {sem.year}-{sem.period}</span>;
    if (sem.status === 'IN_PROGRESS') return <span className="badge bg-success">✅ {sem.year}-{sem.period}</span>;
    return <span className="badge bg-secondary">📅 {sem.year}-{sem.period}</span>;
  };

  const handleDeleteSection = async (sectionId) => {
    if (!window.confirm('¿Eliminar esta sección? Esta acción no se puede deshacer.')) return;
    try {
      await api.delete(`/api/sections/${sectionId}`);
      loadData();
    } catch (err) {
      alert(err.response?.data || 'Error al eliminar la sección.');
    }
  };

  // Filtrar por semestre activo o todos
  const visible = filter === 'active'
    ? sections.filter((s) => s.semesterId === activeSemId)
    : sections;

  if (loading) return <p className="text-muted p-4">Cargando...</p>;
  if (error)   return <div className="alert alert-danger m-4">{error}</div>;

  return (
    <div className="container py-4">
      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-outline-secondary" onClick={() => navigate('/professors')}>
          ← Volver a Profesores
        </button>
        <div>
          <h2 className="fw-bold mb-0">
            Cursos de {professor?.firstName} {professor?.lastName}
          </h2>
          <p className="text-muted mb-0 small">
            {professor?.department} · {visible.length} sección(es) mostrada(s)
          </p>
        </div>
      </div>

      {/* Filtro */}
      <div className="d-flex gap-2 mb-4">
        <button
          className={`btn btn-sm ${filter === 'active' ? 'btn-primary' : 'btn-outline-primary'}`}
          onClick={() => setFilter('active')}>
          ✅ Solo semestre activo
        </button>
        <button
          className={`btn btn-sm ${filter === 'all' ? 'btn-secondary' : 'btn-outline-secondary'}`}
          onClick={() => setFilter('all')}>
          📋 Todos los semestres
        </button>
      </div>

      {visible.length === 0 && (
        <div className="alert alert-info">
          {filter === 'active'
            ? 'Este profesor no tiene secciones en el semestre activo.'
            : 'Este profesor no tiene secciones asignadas.'}
        </div>
      )}

      <div className="card shadow-sm border-0">
        <div className="table-responsive">
          <table className="table table-hover mb-0 align-middle">
            <thead className="table-light">
              <tr>
                <th>#</th>
                <th>Asignatura</th>
                <th>Semestre</th>
                <th>Sala / Horario</th>
                <th>Inscritos</th>
                <th className="text-end">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((s) => {
                const sem     = getSemester(s.semesterId);
                const cerrado = sem?.status === 'CLOSED';
                return (
                  <tr key={s.id} className={cerrado ? 'opacity-75' : ''}>
                    <td className="text-muted small">{s.id}</td>
                    <td>
                      <span className="badge bg-primary font-monospace me-1">{subjectCode(s.subjectId)}</span>
                      <span className="fw-semibold small">{subjectName(s.subjectId)}</span>
                    </td>
                    <td>{semesterBadge(sem)}</td>
                    <td className="small text-muted">
                      {roomName(s.roomId)}
                      {s.dayOfWeek != null && (
                        <div style={{ fontSize: '0.72rem' }}>
                          {DAY_NAMES[s.dayOfWeek]} {s.startTime?.slice(0,5)}–{s.endTime?.slice(0,5)}
                        </div>
                      )}
                    </td>
                    <td className="small text-center">
                      <span className={s.availableSeats === 0 ? 'text-danger fw-bold' : ''}>
                        {s.totalSeats - s.availableSeats}
                      </span>
                      <span className="text-muted">/{s.totalSeats}</span>
                    </td>
                    <td className="text-end d-flex gap-1 justify-content-end">
                      <button
                        className="btn btn-sm btn-outline-primary"
                        onClick={() => navigate(`/sections/edit/${s.id}`)}>
                        Editar
                      </button>
                      <button
                        className="btn btn-sm btn-outline-danger"
                        onClick={() => handleDeleteSection(s.id)}>
                        Eliminar
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
