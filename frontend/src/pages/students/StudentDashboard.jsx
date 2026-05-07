import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

export default function StudentDashboard() {
  const { user }              = useAuth();
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  useEffect(() => {
    api.get(`/api/students/${user.id}`)
      .then((r) => setStudent(r.data))
      .catch(() => setError('No se pudo cargar tu perfil.'))
      .finally(() => setLoading(false));
  }, [user.id]);

  if (loading) return <div className="container py-5 text-muted">Cargando...</div>;
  if (error)   return <div className="container py-5"><div className="alert alert-danger">{error}</div></div>;

  return (
    <div className="container py-4">
      <div
        className="rounded-4 p-4 mb-4 text-white"
        style={{ background: 'linear-gradient(135deg, #003366, #0077cc)' }}
      >
        <h2 className="fw-bold mb-1">Hola, {student?.firstName} 👋</h2>
        <p className="mb-0 opacity-75">
          Matrícula: <code className="text-white">{student?.enrollmentNumber}</code>
          {' · '}
          Estado: <strong>{student?.academicStatus}</strong>
        </p>
      </div>

      <div className="row g-3">
        <div className="col-12 col-md-3">
          <Link to="/my-curriculum" className="text-decoration-none">
            <div className="card border-0 shadow-sm h-100 p-4 text-center">
              <div style={{ fontSize: '2.5rem' }}>📋</div>
              <h5 className="fw-bold mt-2 mb-1">Mi Malla</h5>
              <p className="text-muted small mb-0">Ver el estado de tus asignaturas</p>
            </div>
          </Link>
        </div>

        <div className="col-12 col-md-3">
          <Link to="/my-enrollments" className="text-decoration-none">
            <div className="card border-0 shadow-sm h-100 p-4 text-center">
              <div style={{ fontSize: '2.5rem' }}>📚</div>
              <h5 className="fw-bold mt-2 mb-1">Mis Inscripciones</h5>
              <p className="text-muted small mb-0">Ver cursos inscritos este semestre</p>
            </div>
          </Link>
        </div>

        <div className="col-12 col-md-3">
          <Link to="/my-profile" className="text-decoration-none">
            <div className="card border-0 shadow-sm h-100 p-4 text-center">
              <div style={{ fontSize: '2.5rem' }}>👤</div>
              <h5 className="fw-bold mt-2 mb-1">Mi Perfil</h5>
              <p className="text-muted small mb-0">Ver tu información personal</p>
            </div>
          </Link>
        </div>

        <div className="col-12 col-md-3">
          <Link to="/my-grades" className="text-decoration-none">
            <div className="card border-0 shadow-sm h-100 p-4 text-center">
              <div style={{ fontSize: '2.5rem' }}>🎯</div>
              <h5 className="fw-bold mt-2 mb-1">Mis Notas</h5>
              <p className="text-muted small mb-0">Ver tus calificaciones por asignatura</p>
            </div>
          </Link>
        </div>
      </div>
    </div>
  );
}