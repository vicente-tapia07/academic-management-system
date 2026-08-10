import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

const DAY_NAMES = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

export default function ProfessorCourses() {
  const { user }    = useAuth();
  const navigate    = useNavigate();

  const [sections,   setSections]   = useState([]);
  const [subjects,   setSubjects]   = useState([]);
  const [semesters,  setSemesters]  = useState([]);
  const [filter,     setFilter]     = useState('active'); // 'active' | 'all'
  const [loading,    setLoading]    = useState(true);
  const [error,      setError]      = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const profRes = await api.get('/api/professors');
        const me = profRes.data.find((p) => p.usuarioId === user.id);
        if (!me) throw new Error('Profesor no encontrado');

        const [secRes, subRes, semRes] = await Promise.all([
          api.get(`/api/professors/${me.id}/sections`),
          api.get('/api/subjects'),
          api.get('/api/semesters'),
        ]);
        setSections(secRes.data);
        setSubjects(subRes.data);
        setSemesters(semRes.data);
      } catch {
        setError('Error al cargar los cursos.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [user.id]);

  const subjectName = (id) => subjects.find((s) => s.id === id)?.name ?? `Asignatura #${id}`;
  const subjectCode = (id) => subjects.find((s) => s.id === id)?.code ?? '---';
  const getSemester = (id) => semesters.find((s) => s.id === id);

  const semesterBadge = (sem) => {
    if (!sem) return <span className="badge bg-secondary">—</span>;
    if (sem.status === 'CLOSED')      return <span className="badge bg-dark">🔒 {sem.year}-{sem.period}</span>;
    if (sem.status === 'IN_PROGRESS') return <span className="badge bg-success">✅ {sem.year}-{sem.period}</span>;
    return <span className="badge bg-secondary">📅 {sem.year}-{sem.period}</span>;
  };

  // Filtrar por semestre activo o mostrar todos
  const activeSemesterId = semesters.find((s) => s.status === 'IN_PROGRESS')?.id;
  const visible = filter === 'active'
    ? sections.filter((s) => s.semesterId === activeSemesterId)
    : sections;

  if (loading) return <p className="text-muted p-4">Cargando cursos...</p>;
  if (error)   return <div className="alert alert-danger m-4">{error}</div>;

  return (
    <div className="container py-4">
      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-outline-secondary" onClick={() => navigate('/professor')}>
          ← Volver
        </button>
        <div>
          <h2 className="fw-bold mb-0">Mis Cursos</h2>
          <p className="text-muted mb-0">Secciones asignadas · {visible.length} mostradas</p>
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
            ? 'No tienes secciones en el semestre activo.'
            : 'No tienes secciones asignadas.'}
        </div>
      )}

      <div className="row g-3">
        {visible.map((s) => {
          const sem     = getSemester(s.semesterId);
          const cerrado = sem?.status === 'CLOSED';

          return (
            <div key={s.id} className="col-md-6">
              <div className={`card border-0 shadow-sm h-100 ${cerrado ? 'opacity-75' : ''}`}>
                <div className="card-body p-3">
                  <div className="d-flex justify-content-between align-items-start mb-2">
                    <span className="badge bg-primary font-monospace fs-6">
                      {subjectCode(s.subjectId)}
                    </span>
                    {semesterBadge(sem)}
                  </div>

                  <h5 className="fw-bold mt-1 mb-2">{subjectName(s.subjectId)}</h5>

                  <div className="text-muted small mb-1">
                    🗓️ <strong>{s.dayOfWeek != null ? DAY_NAMES[s.dayOfWeek] : '—'}</strong>
                    {s.startTime ? ` ${s.startTime.slice(0,5)}–${s.endTime?.slice(0,5)}` : ''}
                  </div>
                  <div className="text-muted small mb-1">
                    🚪 {s.room?.name ?? '—'}
                  </div>
                  <div className="text-muted small mb-3">
                    👥 {s.totalSeats - s.availableSeats} / {s.totalSeats} inscritos
                  </div>

                  {cerrado && (
                    <div className="alert alert-secondary py-1 px-2 small mb-2">
                      🔒 Semestre cerrado — notas bloqueadas
                    </div>
                  )}

                  <button
                    className={`btn w-100 ${cerrado ? 'btn-outline-secondary' : 'btn-primary'}`}
                    disabled={cerrado}
                    onClick={() => !cerrado && navigate(`/professor/grades/${s.id}`)}>
                    📋 {cerrado ? 'Notas bloqueadas' : 'Ver y gestionar notas'}
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
