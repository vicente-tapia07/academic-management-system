import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

export default function ProfessorDashboard() {
  const { user } = useAuth();
  const navigate  = useNavigate();

  const [professor, setProfessor] = useState(null);
  const [sections,  setSections]  = useState([]);
  const [loading,   setLoading]   = useState(true);
  const [error,     setError]     = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        // 1. Buscar el profesor vinculado a este usuario
        const profRes = await api.get('/api/professors');
        const all     = profRes.data;
        // Buscar el que coincide con el usuario logueado por usuario_id
        const me = all.find((p) => p.usuarioId === user.id);
        if (!me) throw new Error('Profesor no encontrado');
        setProfessor(me);

        // 2. Cargar sus secciones
        const secRes = await api.get(`/api/professors/${me.id}/sections`);
        setSections(secRes.data);
      } catch (e) {
        setError('Error al cargar el dashboard del profesor.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [user.id]);

  if (loading) return <p className="text-muted p-4">Cargando...</p>;
  if (error)   return <div className="alert alert-danger m-4">{error}</div>;

  return (
    <div className="container py-4">
      {/* Encabezado */}
      <div className="card shadow-sm border-0 mb-4">
        <div className="card-body d-flex align-items-center gap-3">
          <div className="rounded-circle bg-primary text-white d-flex align-items-center
                          justify-content-center" style={{ width: 56, height: 56, fontSize: 22 }}>
            👨‍🏫
          </div>
          <div>
            <h4 className="mb-0 fw-bold">
              {professor.firstName} {professor.lastName}
            </h4>
            <span className="text-muted">{professor.department}</span>
          </div>
        </div>
      </div>

      {/* Resumen */}
      <div className="row g-3 mb-4">
        <div className="col-md-4">
          <div className="card border-0 shadow-sm text-center py-3">
            <div className="fs-1 fw-bold text-primary">{sections.length}</div>
            <div className="text-muted">Secciones activas</div>
          </div>
        </div>
      </div>

      {/* Mis cursos */}
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h5 className="fw-bold mb-0">Mis Cursos</h5>
        <button className="btn btn-outline-primary btn-sm"
          onClick={() => navigate('/professor/courses', { state: { professorId: professor.id } })}>
          Ver todos →
        </button>
      </div>

      <div className="row g-3">
        {sections.length === 0 && (
          <p className="text-muted">No tienes secciones asignadas.</p>
        )}
        {sections.slice(0, 3).map((s) => (
          <div key={s.id} className="col-md-4">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <div className="text-muted small mb-1">Sección #{s.id}</div>
                <div className="fw-semibold">Asignatura ID: {s.subjectId}</div>
                <div className="text-muted small">
                  Cupos: {s.availableSeats}/{s.totalSeats}
                </div>
                <button
                  className="btn btn-sm btn-outline-secondary mt-3 w-100"
                  onClick={() => navigate(`/professor/grades/${s.id}`)}>
                  Ver notas
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}