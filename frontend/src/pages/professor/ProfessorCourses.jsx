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
        // Encontrar el professor_id del usuario logueado
        const profRes = await api.get('/api/professors');
        const me = profRes.data.find((p) => p.usuarioId === user.id);
        if (!me) throw new Error('Profesor no encontrado');

        // Cargar secciones + datos de apoyo en paralelo
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

  // Helpers para mostrar nombres en vez de IDs
  const subjectName  = (id) => subjects.find((s) => s.id === id)?.name  ?? `Asignatura #${id}`;
  const subjectCode  = (id) => subjects.find((s) => s.id === id)?.code  ?? '---';
  const semesterName = (id) => {
    const s = semesters.find((s) => s.id === id);
    return s ? `${s.year} - ${s.period}` : `Semestre #${id}`;
  };

  if (loading) return <p className="text-muted p-4">Cargando cursos...</p>;
  if (error)   return <div className="alert alert-danger m-4">{error}</div>;

  return (
    <div className="container py-4">
      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-outline-secondary"
          onClick={() => navigate('/professor')}>
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
        {sections.map((s) => (
          <div key={s.id} className="col-md-6">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <div className="d-flex justify-content-between align-items-start">
                  <span className="badge bg-primary font-monospace fs-6">
                    {subjectCode(s.subjectId)}
                  </span>
                  <span className="badge bg-secondary">
                    {semesterName(s.semesterId)}
                  </span>
                </div>
                <h5 className="fw-bold mt-2 mb-1">{subjectName(s.subjectId)}</h5>
                <p className="text-muted small mb-3">
                  Cupos disponibles: {s.availableSeats} / {s.totalSeats}
                </p>
                <button
                  className="btn btn-primary w-100"
                  onClick={() => navigate(`/professor/grades/${s.id}`)}>
                  📋 Ver y gestionar notas
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}