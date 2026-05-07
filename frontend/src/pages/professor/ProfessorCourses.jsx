import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

export default function ProfessorCourses() {
  const { user }    = useAuth();
  const navigate    = useNavigate();
  const [sections,  setSections]  = useState([]);
  const [subjects,  setSubjects]  = useState([]);
  const [semesters, setSemesters] = useState([]);
  const [loading,   setLoading]   = useState(true);
  const [error,     setError]     = useState('');

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

  // Ahora devuelve el objeto completo del semestre, no solo el nombre
  const getSemester = (id) => semesters.find((s) => s.id === id);

  // Badge visual según el estado del semestre
  const semesterBadge = (sem) => {
    if (!sem) return <span className="badge bg-secondary">—</span>;
    if (sem.status === 'CLOSED')
      return <span className="badge bg-dark">🔒 {sem.year}-{sem.period} Cerrado</span>;
    if (sem.status === 'IN_PROGRESS')
      return <span className="badge bg-success">✅ {sem.year}-{sem.period} En Curso</span>;
    return <span className="badge bg-secondary">📅 {sem.year}-{sem.period} Planificado</span>;
  };

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
          <p className="text-muted mb-0">Secciones asignadas a ti</p>
        </div>
      </div>

      {sections.length === 0 && (
        <div className="alert alert-info">No tienes secciones asignadas.</div>
      )}

      <div className="row g-3">
        {sections.map((s) => {
          const sem     = getSemester(s.semesterId);
          const cerrado = sem?.status === 'CLOSED';

          return (
            <div key={s.id} className="col-md-6">
              {/* Si el semestre está cerrado, el borde de la card es gris */}
              <div className={`card border-0 shadow-sm h-100 ${cerrado ? 'opacity-75' : ''}`}>
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start">
                    <span className="badge bg-primary font-monospace fs-6">
                      {subjectCode(s.subjectId)}
                    </span>
                    {semesterBadge(sem)}
                  </div>

                  <h5 className="fw-bold mt-2 mb-1">{subjectName(s.subjectId)}</h5>
                  <p className="text-muted small mb-3">
                    Cupos disponibles: {s.availableSeats} / {s.totalSeats}
                  </p>

                  {/* Aviso cuando el semestre está cerrado */}
                  {cerrado && (
                    <div className="alert alert-secondary py-1 px-2 small mb-2">
                      🔒 Semestre cerrado — las notas no pueden modificarse
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