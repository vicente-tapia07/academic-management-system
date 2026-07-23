import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

const DAY_NAMES = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

export default function ProfessorDashboard() {
  const { user }  = useAuth();
  const navigate  = useNavigate();

  const [professor,       setProfessor]       = useState(null);
  const [activeSections,  setActiveSections]  = useState([]);  // semestre activo
  const [subjects,        setSubjects]        = useState([]);
  const [rooms,           setRooms]           = useState([]);
  const [totalSections,   setTotalSections]   = useState(0);
  const [loading,         setLoading]         = useState(true);
  const [error,           setError]           = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const profRes = await api.get('/api/professors');
        const me = profRes.data.find((p) => p.usuarioId === user.id);
        if (!me) throw new Error('Profesor no encontrado');
        setProfessor(me);

        const [activeRes, allRes, subRes, roomRes] = await Promise.all([
          api.get(`/api/sections/professor/${me.id}/active`),
          api.get(`/api/professors/${me.id}/sections`),
          api.get('/api/subjects'),
          api.get('/api/rooms'),
        ]);

        setActiveSections(activeRes.data);
        setTotalSections(allRes.data.length);
        setSubjects(subRes.data);
        setRooms(roomRes.data);
      } catch {
        setError('Error al cargar el dashboard.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [user.id]);

  const subjectName = (id) => subjects.find((s) => s.id === id)?.name ?? `Asignatura #${id}`;
  const subjectCode = (id) => subjects.find((s) => s.id === id)?.code ?? '---';
  const roomName    = (id) => rooms.find((r) => r.id === id)?.name ?? (id ? `Sala #${id}` : '—');

  if (loading) return <p className="text-muted p-4">Cargando...</p>;
  if (error)   return <div className="alert alert-danger m-4">{error}</div>;

  return (
    <div className="container py-4">

      {/* Encabezado profesor */}
      <div className="card shadow-sm border-0 mb-4">
        <div className="card-body d-flex align-items-center gap-3 p-4">
          <div className="rounded-circle bg-primary text-white d-flex align-items-center
                          justify-content-center fw-bold"
               style={{ width: 60, height: 60, fontSize: 26 }}>
            👨‍🏫
          </div>
          <div>
            <h4 className="mb-0 fw-bold">{professor.firstName} {professor.lastName}</h4>
            <span className="text-muted">{professor.department}</span>
          </div>
        </div>
      </div>

      {/* Estadísticas */}
      <div className="row g-3 mb-4">
        <div className="col-md-4">
          <div className="card border-0 shadow-sm text-center py-3">
            <div className="fs-1 fw-bold text-primary">{activeSections.length}</div>
            <div className="text-muted small">Secciones semestre activo</div>
          </div>
        </div>
        <div className="col-md-4">
          <div className="card border-0 shadow-sm text-center py-3">
            <div className="fs-1 fw-bold text-secondary">{totalSections}</div>
            <div className="text-muted small">Total secciones históricas</div>
          </div>
        </div>
        <div className="col-md-4">
          <div className="card border-0 shadow-sm text-center py-3">
            <div className="fs-1 fw-bold text-success">
              {activeSections.reduce((acc, s) => acc + (s.totalSeats - s.availableSeats), 0)}
            </div>
            <div className="text-muted small">Estudiantes inscritos (semestre activo)</div>
          </div>
        </div>
      </div>

      {/* Secciones del semestre activo */}
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h5 className="fw-bold mb-0">Secciones del Semestre Activo</h5>
        <div className="d-flex gap-2">
          <button className="btn btn-outline-secondary btn-sm"
            onClick={() => navigate('/professor/schedule')}>
            🗓️ Ver Horario
          </button>
          <button className="btn btn-outline-primary btn-sm"
            onClick={() => navigate('/professor/courses')}>
            Ver todos los cursos →
          </button>
        </div>
      </div>

      {activeSections.length === 0 ? (
        <div className="alert alert-info">No tienes secciones en el semestre activo.</div>
      ) : (
        <div className="row g-3">
          {activeSections.slice(0, 6).map((s) => (
            <div key={s.id} className="col-md-4">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-body p-3">
                  <div className="d-flex justify-content-between align-items-start mb-2">
                    <span className="badge bg-primary font-monospace">{subjectCode(s.subjectId)}</span>
                    <span className="badge bg-success">Activa</span>
                  </div>
                  <div className="fw-semibold mb-2">{subjectName(s.subjectId)}</div>
                  <div className="text-muted small mb-1">
                    🗓️ {s.dayOfWeek != null ? DAY_NAMES[s.dayOfWeek] : '—'}
                    {s.startTime ? ` ${s.startTime.slice(0,5)}–${s.endTime?.slice(0,5)}` : ''}
                  </div>
                  <div className="text-muted small mb-2">
                    🚪 {roomName(s.roomId)}
                  </div>
                  <div className="text-muted small mb-3">
                    👥 {s.totalSeats - s.availableSeats} / {s.totalSeats} inscritos
                  </div>
                  <button className="btn btn-sm btn-outline-primary w-100"
                    onClick={() => navigate(`/professor/grades/${s.id}`)}>
                    Ver notas
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
